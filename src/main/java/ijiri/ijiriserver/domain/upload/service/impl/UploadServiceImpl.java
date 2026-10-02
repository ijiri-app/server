package ijiri.ijiriserver.domain.upload.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.upload.client.ImageStorageClient;
import ijiri.ijiriserver.domain.upload.dto.AttachedImage;
import ijiri.ijiriserver.domain.upload.dto.StoredObject;
import ijiri.ijiriserver.domain.upload.dto.request.UploadUrlRequest;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;
import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.domain.upload.repository.UploadedImageRepository;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import ijiri.ijiriserver.global.storage.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final String UPLOAD_KEY_PREFIX = "upload:member:";
    private static final int UPLOAD_LIMIT_PER_MEMBER = 100;
    private static final Duration UPLOAD_LIMIT_PERIOD = Duration.ofHours(1);
    private static final Duration UPLOAD_URL_VALIDITY = Duration.ofMinutes(10);
    // R2 수명 주기 규칙(tmp/, 1일)과 같은 기준으로 연결되지 않은 업로드 기록을 지운다
    private static final Duration UNATTACHED_RETENTION = Duration.ofDays(1);
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int SIGNATURE_BYTES = 12;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageStorageClient imageStorageClient;
    private final ImageUrlResolver imageUrlResolver;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    @Override
    @Transactional
    public UploadResponse createUploadUrls(Long memberId, UploadUrlRequest request) {
        for (int i = 0; i < request.count(); i++) {
            rateLimiter.check(UPLOAD_KEY_PREFIX + memberId, UPLOAD_LIMIT_PER_MEMBER, UPLOAD_LIMIT_PERIOD);
        }
        String extension = EXTENSIONS.get(request.contentType());
        List<UploadResponse.Item> items = IntStream.range(0, request.count())
                .mapToObj(ignored -> {
                    String imageKey = request.purpose().getKeyPrefix() + UUID.randomUUID() + "." + extension;
                    uploadedImageRepository.save(UploadedImage.builder()
                            .memberId(memberId)
                            .imageKey(imageKey)
                            .contentType(request.contentType())
                            .status(UploadStatus.PENDING)
                            .build()
                    );
                    return new UploadResponse.Item(
                            imageKey,
                            imageStorageClient.createUploadUrl(imageKey, request.contentType(), UPLOAD_URL_VALIDITY),
                            UPLOAD_URL_VALIDITY.toSeconds()
                    );
                })
                .toList();
        return new UploadResponse(items);
    }

    // 서버는 파일을 거치지 않으므로 연결할 때 저장소에서 크기·형식(HEAD)과 파일 앞부분(시그니처)을 확인한 뒤
    // 임시 키(tmp/)에서 영구 키로 옮긴다. 임시 키는 수명 주기 규칙으로 지워지므로 옮기지 않으면 사라진다
    @Override
    @Transactional
    public List<AttachedImage> attach(Long memberId, List<String> imageKeys, UploadPurpose purpose) {
        if (imageKeys.isEmpty()) {
            return List.of();
        }
        Map<String, UploadedImage> images = uploadedImageRepository.findAllByImageKeyIn(imageKeys).stream()
                .filter(image -> image.getMemberId().equals(memberId))
                .filter(image -> image.getImageKey().startsWith(purpose.getKeyPrefix()))
                .filter(image -> !image.isAttached())
                .collect(Collectors.toMap(UploadedImage::getImageKey, Function.identity()));
        if (new HashSet<>(imageKeys).size() != imageKeys.size() || images.size() != imageKeys.size()) {
            throw new CustomException(UploadStatusCode.INVALID_IMAGE);
        }
        images.values().forEach(this::validateStoredFile);
        return imageKeys.stream()
                .map(images::get)
                .map(this::moveToPermanentKey)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Collection<String> imageKeys) {
        deleteAll(uploadedImageRepository.findAllByImageKeyIn(imageKeys));
    }

    @Override
    @Transactional
    public int deleteUnattached() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(UNATTACHED_RETENTION);
        List<UploadedImage> images = uploadedImageRepository.findAllByStatusNotAndCreatedAtBefore(
                UploadStatus.ATTACHED,
                cutoff
        );
        deleteAll(images);
        return images.size();
    }

    // 게시물을 지운 뒤 남은 업로드 기록과 파일(프로필 사진 포함)을 저장소에서 지운다.
    // 이후 롤백되면 남은 행을 다음 스케줄에 다시 지우고, 이미 없는 파일은 무시하므로 여러 번 실행돼도 안전하다
    @Order(2)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        deleteAll(uploadedImageRepository.findAllByMemberId(event.memberId()));
    }

    // 새 프로필 사진은 이 회원이 PROFILE 용도로 받은 업로드여야 한다. 아니면 예외로 프로필 변경을 롤백시킨다
    @EventListener
    @Transactional
    public void changeProfileImage(MemberProfileImageChangedEvent event) {
        if (event.newImageKey() != null) {
            attach(event.memberId(), List.of(event.newImageKey()), UploadPurpose.PROFILE);
        }
        // 소셜 프로필 사진처럼 직접 올리지 않은 이전 사진은 지울 파일이 없다
        imageUrlResolver.keyOf(event.previousUrl())
                .flatMap(uploadedImageRepository::findByImageKey)
                .filter(image -> image.getMemberId().equals(event.memberId()))
                .ifPresent(image -> deleteAll(List.of(image)));
    }

    private void validateStoredFile(UploadedImage image) {
        StoredObject stored = imageStorageClient.head(image.getImageKey())
                .orElseThrow(() -> new CustomException(UploadStatusCode.INVALID_IMAGE));
        boolean valid = stored.size() > 0
                && stored.size() <= MAX_FILE_BYTES
                && image.getContentType().equals(stored.contentType())
                && matchesSignature(image.getContentType(), imageStorageClient.readPrefix(
                        image.getImageKey(),
                        SIGNATURE_BYTES
                ));
        if (!valid) {
            throw new CustomException(UploadStatusCode.INVALID_IMAGE);
        }
    }

    private AttachedImage moveToPermanentKey(UploadedImage image) {
        String uploadKey = image.getImageKey();
        String permanentKey = imageUrlResolver.permanentKeyOf(uploadKey);
        imageStorageClient.copy(uploadKey, permanentKey);
        imageStorageClient.delete(uploadKey);
        image.attach(permanentKey);
        return new AttachedImage(permanentKey, imageUrlResolver.urlOf(permanentKey));
    }

    private void deleteAll(List<UploadedImage> images) {
        uploadedImageRepository.deleteAll(images);
        images.forEach(image -> imageStorageClient.delete(image.getImageKey()));
    }

    private boolean matchesSignature(String contentType, byte[] head) {
        return switch (contentType) {
            case "image/jpeg" -> head.length >= 3
                    && Arrays.equals(head, 0, 3, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 0, 3);
            case "image/webp" -> ascii(head, 0, 4).equals("RIFF") && ascii(head, 8, 4).equals("WEBP");
            default -> false;
        };
    }

    private String ascii(byte[] content, int offset, int length) {
        if (content.length < offset + length) {
            return "";
        }
        return new String(content, offset, length, StandardCharsets.US_ASCII);
    }
}

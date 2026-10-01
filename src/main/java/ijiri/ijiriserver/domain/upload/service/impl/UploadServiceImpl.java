package ijiri.ijiriserver.domain.upload.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.upload.client.DbImageStorageClient;
import ijiri.ijiriserver.domain.upload.client.ImageStorageClient;
import ijiri.ijiriserver.domain.upload.dto.AttachedImage;
import ijiri.ijiriserver.domain.upload.dto.StoredImage;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Duration UNATTACHED_RETENTION = Duration.ofMinutes(10);
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic"
    );
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1");

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageStorageClient imageStorageClient;
    // 서명 확인과 파일 저장·조회는 DB 저장소에만 있는 기능이다. S3 등으로 옮기면 저장소가 직접 받는다
    private final DbImageStorageClient dbImageStorageClient;
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

    // Content-Type 은 위조할 수 있으므로 파일 앞부분(시그니처)도 확인한다. JPEG 는 위치 정보가 남지 않도록 EXIF 를 지운다
    @Override
    @Transactional
    public void receiveFile(String imageKey, long expires, String signature, String contentType, byte[] content) {
        UploadedImage image = uploadedImageRepository.findByImageKey(imageKey)
                .filter(found -> found.getContentType().equals(contentType))
                .filter(found -> dbImageStorageClient.isValidSignature(imageKey, contentType, expires, signature))
                .orElseThrow(() -> new CustomException(UploadStatusCode.INVALID_UPLOAD_URL));
        if (image.getStatus() != UploadStatus.PENDING) {
            throw new CustomException(UploadStatusCode.INVALID_UPLOAD_URL);
        }
        if (!matchesSignature(contentType, content)) {
            throw new CustomException(UploadStatusCode.UNSUPPORTED_IMAGE);
        }
        byte[] stored = contentType.equals("image/jpeg") ? stripJpegMetadata(content) : content;
        dbImageStorageClient.save(imageKey, contentType, stored);
        image.markUploaded();
    }

    @Override
    @Transactional(readOnly = true)
    public StoredImage loadImage(String imageKey) {
        return dbImageStorageClient.load(imageKey)
                .map(file -> new StoredImage(file.getContentType(), file.getContent()))
                .orElseThrow(() -> new CustomException(UploadStatusCode.IMAGE_NOT_FOUND));
    }

    @Override
    @Transactional
    public List<AttachedImage> attach(Long memberId, List<String> imageKeys, UploadPurpose purpose) {
        if (imageKeys.isEmpty()) {
            return List.of();
        }
        Map<String, UploadedImage> images = uploadedImageRepository.findAllByImageKeyIn(imageKeys).stream()
                .filter(image -> image.getMemberId().equals(memberId))
                .filter(image -> image.getImageKey().startsWith(purpose.getKeyPrefix()))
                .filter(image -> image.isUploaded() && imageStorageClient.exists(image.getImageKey()))
                .collect(Collectors.toMap(UploadedImage::getImageKey, Function.identity()));
        if (new HashSet<>(imageKeys).size() != imageKeys.size() || images.size() != imageKeys.size()) {
            throw new CustomException(UploadStatusCode.INVALID_IMAGE);
        }
        return imageKeys.stream()
                .map(images::get)
                .peek(UploadedImage::attach)
                .map(image -> new AttachedImage(image.getImageKey(), imageUrlResolver.urlOf(image.getImageKey())))
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

    // 게시물을 지운 뒤 남은 업로드 기록과 파일을 지운다. 이후 롤백되면 남은 행을 다음 스케줄에 다시 지우고,
    // 이미 없는 파일은 무시하므로 여러 번 실행돼도 안전하다
    @Order(2)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        deleteAll(uploadedImageRepository.findAllByMemberId(event.memberId()));
    }

    // 새 프로필 사진은 이 회원이 PROFILE 용도로 올리고 아직 쓰이지 않은 사진이어야 한다. 아니면 예외로 프로필 변경을 롤백시킨다
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

    private void deleteAll(List<UploadedImage> images) {
        uploadedImageRepository.deleteAll(images);
        images.forEach(image -> imageStorageClient.delete(image.getImageKey()));
    }

    private boolean matchesSignature(String contentType, byte[] content) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/png" -> startsWith(content, PNG_SIGNATURE);
            case "image/webp" -> ascii(content, 0, 4).equals("RIFF") && ascii(content, 8, 4).equals("WEBP");
            case "image/heic" -> ascii(content, 4, 4).equals("ftyp") && HEIC_BRANDS.contains(ascii(content, 8, 4));
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }

    private String ascii(byte[] content, int offset, int length) {
        if (content.length < offset + length) {
            return "";
        }
        return new String(content, offset, length, StandardCharsets.US_ASCII);
    }

    // JPEG 의 APP1 세그먼트(EXIF, XMP: 위치 정보·촬영 기기 등)를 빼고 나머지는 그대로 둔다.
    // 앱이 다시 인코딩하며 회전을 반영해 올리므로 EXIF 방향 정보가 없어도 된다. 구조가 이상하면 원본을 그대로 쓴다
    private byte[] stripJpegMetadata(byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
        out.write(content, 0, 2);
        int position = 2;
        while (position + 4 <= content.length && (content[position] & 0xFF) == 0xFF) {
            int marker = content[position + 1] & 0xFF;
            // SOS 부터는 압축된 이미지 데이터라 그대로 복사한다
            if (marker == 0xDA) {
                break;
            }
            int length = ((content[position + 2] & 0xFF) << 8) | (content[position + 3] & 0xFF);
            int segmentEnd = position + 2 + length;
            if (length < 2 || segmentEnd > content.length) {
                return content;
            }
            if (marker != 0xE1) {
                out.write(content, position, segmentEnd - position);
            }
            position = segmentEnd;
        }
        out.write(content, position, content.length - position);
        return out.toByteArray();
    }
}

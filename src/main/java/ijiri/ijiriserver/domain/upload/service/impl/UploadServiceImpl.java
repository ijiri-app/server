package ijiri.ijiriserver.domain.upload.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.upload.client.ImageStorageClient;
import ijiri.ijiriserver.domain.upload.dto.ImageInfo;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.domain.upload.repository.UploadedImageRepository;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final String UPLOAD_KEY_PREFIX = "upload:member:";
    private static final int UPLOAD_LIMIT_PER_MEMBER = 100;
    private static final Duration UPLOAD_LIMIT_PERIOD = Duration.ofHours(1);
    private static final long UNATTACHED_RETENTION_HOURS = 24;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1");

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageStorageClient imageStorageClient;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    // 확장자나 Content-Type 은 위조할 수 있으므로 파일 앞부분(시그니처)으로 형식을 판단한다
    @Override
    @Transactional
    public UploadResponse uploadImage(Long memberId, byte[] content) {
        rateLimiter.check(UPLOAD_KEY_PREFIX + memberId, UPLOAD_LIMIT_PER_MEMBER, UPLOAD_LIMIT_PERIOD);
        if (content.length == 0) {
            throw new CustomException(UploadStatusCode.EMPTY_IMAGE);
        }
        String key = UUID.randomUUID() + "." + detectExtension(content);
        int[] size = readSize(content);
        String url = imageStorageClient.store(key, content);
        UploadedImage image = uploadedImageRepository.save(UploadedImage.builder()
                .memberId(memberId)
                .storageKey(key)
                .url(url)
                .width(size != null ? size[0] : null)
                .height(size != null ? size[1] : null)
                .build()
        );
        return UploadResponse.image(image);
    }

    @Override
    @Transactional
    public List<ImageInfo> attach(Long memberId, List<Long> imageIds) {
        Set<Long> distinct = new LinkedHashSet<>(imageIds);
        Map<Long, UploadedImage> images = uploadedImageRepository
                .findAllByIdInAndMemberIdAndAttachedFalse(distinct, memberId).stream()
                .collect(Collectors.toMap(UploadedImage::getId, Function.identity()));
        if (distinct.size() != imageIds.size() || images.size() != imageIds.size()) {
            throw new CustomException(UploadStatusCode.INVALID_IMAGE);
        }
        return imageIds.stream()
                .map(images::get)
                .peek(UploadedImage::attach)
                .map(ImageInfo::from)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Collection<Long> imageIds) {
        deleteAll(uploadedImageRepository.findAllById(imageIds));
    }

    @Override
    @Transactional
    public int deleteUnattached() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(UNATTACHED_RETENTION_HOURS);
        List<UploadedImage> images = uploadedImageRepository.findAllByAttachedFalseAndCreatedAtBefore(cutoff);
        deleteAll(images);
        return images.size();
    }

    // 영구 삭제 트랜잭션 안에서 파일까지 지운다. 이후 롤백되면 남은 행을 다음 스케줄에 다시 지우고,
    // 이미 없는 파일은 무시하므로 여러 번 실행돼도 안전하다
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        deleteAll(uploadedImageRepository.findAllByMemberId(event.memberId()));
    }

    // 새 프로필 사진은 이 회원이 올리고 아직 쓰이지 않은 사진이어야 한다. 아니면 예외로 프로필 변경을 롤백시킨다
    @EventListener
    @Transactional
    public void changeProfileImage(MemberProfileImageChangedEvent event) {
        if (event.newUrl() != null) {
            uploadedImageRepository.findByMemberIdAndUrl(event.memberId(), event.newUrl())
                    .filter(image -> !image.isAttached())
                    .orElseThrow(() -> new CustomException(UploadStatusCode.INVALID_IMAGE))
                    .attach();
        }
        // 소셜 프로필 사진처럼 직접 올리지 않은 이전 사진은 지울 파일이 없다
        if (event.previousUrl() != null) {
            uploadedImageRepository.findByMemberIdAndUrl(event.memberId(), event.previousUrl())
                    .ifPresent(image -> deleteAll(List.of(image)));
        }
    }

    private void deleteAll(List<UploadedImage> images) {
        uploadedImageRepository.deleteAll(images);
        images.forEach(image -> imageStorageClient.delete(image.getStorageKey()));
    }

    private String detectExtension(byte[] content) {
        if (startsWith(content, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "jpg";
        }
        if (startsWith(content, 0, PNG_SIGNATURE)) {
            return "png";
        }
        if (ascii(content, 0, 4).equals("RIFF") && ascii(content, 8, 4).equals("WEBP")) {
            return "webp";
        }
        if (ascii(content, 4, 4).equals("ftyp") && HEIC_BRANDS.contains(ascii(content, 8, 4))) {
            return "heic";
        }
        throw new CustomException(UploadStatusCode.UNSUPPORTED_IMAGE);
    }

    private boolean startsWith(byte[] content, int offset, byte[] signature) {
        return content.length >= offset + signature.length
                && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }

    private String ascii(byte[] content, int offset, int length) {
        if (content.length < offset + length) {
            return "";
        }
        return new String(content, offset, length, StandardCharsets.US_ASCII);
    }

    // 피드 벽돌형 배치에 쓰는 가로세로 크기. 전체를 디코딩하지 않고 헤더만 읽는다.
    // 기본 ImageIO 가 읽지 못하는 형식(WebP, HEIC)은 null
    private int[] readSize(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return null;
        }
    }
}

package ijiri.ijiriserver.domain.upload.client;

import ijiri.ijiriserver.domain.upload.entity.ImageFile;
import ijiri.ijiriserver.domain.upload.repository.ImageFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 사진을 DB(image_file)에 저장하는 임시 저장소. 업로드 URL 은 S3 presigned URL 처럼
 * 키·Content-Type·만료 시각에 대한 HMAC 서명을 붙인 이 서버의 주소(PUT /uploads/files/{key})다.
 */
@Component
public class DbImageStorageClient implements ImageStorageClient {

    private static final String UPLOAD_PATH = "/uploads/files/";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    // JWT 와 같은 비밀값을 쓰되, 용도가 다른 서명이 서로 통하지 않도록 앞에 붙인다
    private static final String SIGNING_CONTEXT = "image-upload:";

    private final ImageFileRepository imageFileRepository;
    private final String publicBaseUrl;
    private final byte[] signingKey;
    private final Clock clock;

    public DbImageStorageClient(
            ImageFileRepository imageFileRepository,
            @Value("${storage.public-base-url}") String publicBaseUrl,
            @Value("${jwt.secret}") String secret,
            Clock clock
    ) {
        this.imageFileRepository = imageFileRepository;
        this.publicBaseUrl = publicBaseUrl;
        this.signingKey = (SIGNING_CONTEXT + secret).getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    public String createUploadUrl(String imageKey, String contentType, Duration validity) {
        long expires = clock.instant().plus(validity).getEpochSecond();
        return publicBaseUrl + UPLOAD_PATH + imageKey
                + "?expires=" + expires
                + "&signature=" + sign(imageKey, contentType, expires);
    }

    @Override
    public boolean exists(String imageKey) {
        return imageFileRepository.existsByImageKey(imageKey);
    }

    @Override
    public void delete(String imageKey) {
        imageFileRepository.deleteByImageKey(imageKey);
    }

    public boolean isValidSignature(String imageKey, String contentType, long expires, String signature) {
        if (expires < clock.instant().getEpochSecond() || signature == null) {
            return false;
        }
        byte[] expected = sign(imageKey, contentType, expires).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    public void save(String imageKey, String contentType, byte[] content) {
        imageFileRepository.save(ImageFile.builder()
                .imageKey(imageKey)
                .contentType(contentType)
                .content(content)
                .build()
        );
    }

    public Optional<ImageFile> load(String imageKey) {
        return imageFileRepository.findByImageKey(imageKey);
    }

    private String sign(String imageKey, String contentType, long expires) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((imageKey + "\n" + contentType + "\n" + expires)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

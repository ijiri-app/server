package ijiri.ijiriserver.domain.upload.client;

import ijiri.ijiriserver.domain.upload.dto.StoredObject;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cloudflare R2(S3 호환 API) 사진 저장소. 앱은 presigned PUT URL 로 직접 올리고 서버는 파일을 거치지 않는다.
 * 공개 URL 은 R2 버킷에 연결한 커스텀 도메인(storage.public-base-url, 예: https://img.ijiri.net)이다.
 * tmp/ 아래 임시 파일은 R2 버킷의 수명 주기 규칙(tmp/ 접두사, 1일)으로도 지워진다.
 * AWS SDK 는 자체 HTTP 클라이언트를 써서 externalApiRequestFactory 대신 SDK 설정으로 타임아웃을 건다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "r2")
public class R2ImageStorageClient implements ImageStorageClient {

    private static final Region REGION = Region.of("auto");
    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(10);
    private static final int NOT_FOUND = 404;

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public R2ImageStorageClient(
            @Value("${storage.r2.account-id}") String accountId,
            @Value("${storage.r2.access-key-id}") String accessKeyId,
            @Value("${storage.r2.secret-access-key}") String secretAccessKey,
            @Value("${storage.r2.bucket}") String bucket
    ) {
        URI endpoint = URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
        S3Configuration pathStyle = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .overrideConfiguration(config -> config.apiCallTimeout(API_CALL_TIMEOUT))
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(REGION)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();
        this.bucket = bucket;
    }

    // Content-Type 이 서명 헤더에 들어가므로 앱은 같은 Content-Type 으로만 올릴 수 있다
    @Override
    public String createUploadUrl(String imageKey, String contentType, Duration validity) {
        return presigner.presignPutObject(request -> request
                .signatureDuration(validity)
                .putObjectRequest(put -> put.bucket(bucket).key(imageKey).contentType(contentType))
        ).url().toString();
    }

    @Override
    public Optional<StoredObject> head(String imageKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(request -> request.bucket(bucket).key(imageKey));
            return Optional.of(new StoredObject(response.contentLength(), response.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return Optional.empty();
            }
            throw storageError("head", imageKey, e);
        } catch (SdkException e) {
            throw storageError("head", imageKey, e);
        }
    }

    @Override
    public byte[] readPrefix(String imageKey, int length) {
        return call("readPrefix", imageKey, () -> s3Client.getObjectAsBytes(request -> request
                .bucket(bucket)
                .key(imageKey)
                .range("bytes=0-" + (length - 1))
        ).asByteArray());
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        call("copy", sourceKey, () -> s3Client.copyObject(request -> request
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(targetKey)
        ));
    }

    // S3 의 DeleteObject 는 없는 키여도 성공한다
    @Override
    public void delete(String imageKey) {
        call("delete", imageKey, () -> s3Client.deleteObject(request -> request.bucket(bucket).key(imageKey)));
    }

    @PreDestroy
    public void close() {
        presigner.close();
        s3Client.close();
    }

    private <T> T call(String operation, String imageKey, Supplier<T> action) {
        try {
            return action.get();
        } catch (SdkException e) {
            throw storageError(operation, imageKey, e);
        }
    }

    private CustomException storageError(String operation, String imageKey, SdkException e) {
        log.error("R2 {} failed: key={}", operation, imageKey, e);
        return new CustomException(UploadStatusCode.STORAGE_ERROR);
    }
}

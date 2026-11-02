package ijiri.ijiriserver.domain.upload.adapter.out.external

import ijiri.ijiriserver.domain.upload.application.data.StoredObject
import ijiri.ijiriserver.domain.upload.application.port.out.ImageStorageClient
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

/**
 * Cloudflare R2(S3 호환 API) 사진 저장소. 앱은 presigned PUT URL 로 직접 올리고 서버는 파일을 거치지 않는다.
 * 공개 URL 은 R2 버킷에 연결한 커스텀 도메인(storage.public-base-url, 예: https://img.ijiri.net)이다.
 * tmp/ 아래 임시 파일은 R2 버킷의 수명 주기 규칙(tmp/ 접두사, 1일)으로도 지워진다.
 * AWS SDK 는 자체 HTTP 클라이언트를 써서 externalApiRequestFactory 대신 SDK 설정으로 타임아웃을 건다.
 */
@Component
@ConditionalOnProperty(name = ["storage.type"], havingValue = "r2")
class R2ImageStorageClient(
    @Value("\${storage.r2.account-id}") accountId: String,
    @Value("\${storage.r2.access-key-id}") accessKeyId: String,
    @Value("\${storage.r2.secret-access-key}") secretAccessKey: String,
    @Value("\${storage.r2.bucket}") private val bucket: String,
) : ImageStorageClient {

    private val s3Client: S3Client
    private val presigner: S3Presigner

    init {
        val endpoint = URI.create("https://$accountId.r2.cloudflarestorage.com")
        val credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        )
        val pathStyle = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build()
        s3Client = S3Client.builder()
            .endpointOverride(endpoint)
            .region(REGION)
            .credentialsProvider(credentials)
            .serviceConfiguration(pathStyle)
            .overrideConfiguration { it.apiCallTimeout(API_CALL_TIMEOUT) }
            .build()
        presigner = S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(REGION)
            .credentialsProvider(credentials)
            .serviceConfiguration(pathStyle)
            .build()
    }

    // Content-Type 이 서명 헤더에 들어가므로 앱은 같은 Content-Type 으로만 올릴 수 있다
    override fun createUploadUrl(imageKey: String, contentType: String, validity: Duration): String {
        return presigner.presignPutObject { request ->
            request
                .signatureDuration(validity)
                .putObjectRequest { it.bucket(bucket).key(imageKey).contentType(contentType) }
        }.url().toString()
    }

    override fun head(imageKey: String): StoredObject? {
        try {
            val response = s3Client.headObject { it.bucket(bucket).key(imageKey) }
            return StoredObject(response.contentLength(), response.contentType())
        } catch (e: NoSuchKeyException) {
            return null
        } catch (e: S3Exception) {
            if (e.statusCode() == NOT_FOUND) {
                return null
            }
            throw storageError("head", imageKey, e)
        } catch (e: SdkException) {
            throw storageError("head", imageKey, e)
        }
    }

    override fun readPrefix(imageKey: String, length: Int): ByteArray {
        return call("readPrefix", imageKey) {
            s3Client.getObjectAsBytes {
                it
                    .bucket(bucket)
                    .key(imageKey)
                    .range("bytes=0-" + (length - 1))
            }.asByteArray()
        }
    }

    override fun copy(sourceKey: String, targetKey: String) {
        call("copy", sourceKey) {
            s3Client.copyObject {
                it
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(targetKey)
            }
        }
    }

    // S3 의 DeleteObject 는 없는 키여도 성공한다
    override fun delete(imageKey: String) {
        call("delete", imageKey) { s3Client.deleteObject { it.bucket(bucket).key(imageKey) } }
    }

    @PreDestroy
    fun close() {
        presigner.close()
        s3Client.close()
    }

    private fun <T> call(operation: String, imageKey: String, action: () -> T): T {
        try {
            return action()
        } catch (e: SdkException) {
            throw storageError(operation, imageKey, e)
        }
    }

    private fun storageError(operation: String, imageKey: String, e: SdkException): CustomException {
        log.error("R2 {} failed: key={}", operation, imageKey, e)
        return CustomException(UploadStatusCode.STORAGE_ERROR)
    }

    companion object {
        private val REGION = Region.of("auto")
        private val API_CALL_TIMEOUT = Duration.ofSeconds(10)
        private const val NOT_FOUND = 404
        private val log = LoggerFactory.getLogger(R2ImageStorageClient::class.java)
    }
}

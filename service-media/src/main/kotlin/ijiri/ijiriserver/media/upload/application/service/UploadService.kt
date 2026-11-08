package ijiri.ijiriserver.media.upload.application.service

import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.UploadPurpose
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.ratelimit.RateLimiter
import ijiri.ijiriserver.common.storage.ImageUrlResolver
import ijiri.ijiriserver.common.transaction.TransactionHooks
import ijiri.ijiriserver.media.upload.application.data.request.UploadUrlRequest
import ijiri.ijiriserver.media.upload.application.data.response.UploadResponse
import ijiri.ijiriserver.media.upload.application.port.`in`.AttachUploadedImagesUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.CreateUploadUrlsUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteMemberImagesUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteUnattachedUploadsUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteUploadedImagesUseCase
import ijiri.ijiriserver.media.upload.application.port.out.ImageStorageClient
import ijiri.ijiriserver.media.upload.application.port.out.UploadedImagePort
import ijiri.ijiriserver.media.upload.entity.UploadStatus
import ijiri.ijiriserver.media.upload.entity.UploadedImage
import ijiri.ijiriserver.media.upload.exception.UploadStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class UploadService(
    private val uploadedImagePort: UploadedImagePort,
    private val imageStorageClient: ImageStorageClient,
    private val imageUrlResolver: ImageUrlResolver,
    private val rateLimiter: RateLimiter,
    private val clock: Clock,
) : CreateUploadUrlsUseCase,
    AttachUploadedImagesUseCase,
    DeleteUploadedImagesUseCase,
    DeleteMemberImagesUseCase,
    DeleteUnattachedUploadsUseCase {

    @Transactional
    override fun createUploadUrls(memberId: Long, request: UploadUrlRequest): UploadResponse {
        val count = request.count!!
        val contentType = request.contentType!!
        repeat(count) {
            rateLimiter.check(UPLOAD_KEY_PREFIX + memberId, UPLOAD_LIMIT_PER_MEMBER, UPLOAD_LIMIT_PERIOD)
        }
        val extension = EXTENSIONS[contentType]
        val items = List(count) {
            val imageKey = request.purpose.keyPrefix + UUID.randomUUID() + "." + extension
            uploadedImagePort.save(
                UploadedImage(
                    memberId = memberId,
                    imageKey = imageKey,
                    contentType = contentType,
                    status = UploadStatus.PENDING,
                )
            )
            UploadResponse.Item(
                imageKey,
                imageStorageClient.createUploadUrl(imageKey, contentType, UPLOAD_URL_VALIDITY),
                UPLOAD_URL_VALIDITY.toSeconds(),
            )
        }
        return UploadResponse(items)
    }

    // 서버는 파일을 거치지 않으므로 연결할 때 저장소에서 크기·형식(HEAD)과 파일 앞부분(시그니처)을 확인한 뒤
    // 임시 키(tmp/)에서 영구 키로 옮긴다. 임시 키는 수명 주기 규칙으로 지워지므로 옮기지 않으면 사라진다.
    // 저장소 작업은 DB 롤백으로 되돌릴 수 없으므로, 복사는 지금 하되 임시 파일 삭제는 커밋 뒤에,
    // 롤백되면 복사본을 지워 같은 업로드 키로 다시 시도할 수 있게 한다
    @Transactional
    override fun attach(memberId: Long, imageKeys: List<String>, purpose: UploadPurpose): List<AttachedImage> {
        if (imageKeys.isEmpty()) {
            return listOf()
        }
        val images = uploadedImagePort.findAllByImageKeyIn(imageKeys)
            .filter { it.memberId == memberId }
            .filter { it.imageKey.startsWith(purpose.keyPrefix) }
            .filter { !it.isAttached() }
            .associateBy { it.imageKey }
        if (imageKeys.toSet().size != imageKeys.size || images.size != imageKeys.size) {
            throw CustomException(UploadStatusCode.INVALID_IMAGE)
        }
        images.values.forEach { validateStoredFile(it) }
        return imageKeys.map { moveToPermanentKey(images.getValue(it)) }
    }

    @Transactional
    override fun delete(imageKeys: Collection<String>) {
        remove(uploadedImagePort.findAllByImageKeyIn(imageKeys))
    }

    // 소셜 프로필 사진처럼 직접 올리지 않은 URL 은 지울 파일이 없다
    @Transactional
    override fun delete(memberId: Long, imageUrls: Collection<String>, imageKeys: Collection<String>) {
        val keys = imageUrls.mapNotNull { imageUrlResolver.keyOf(it) } + imageKeys
        if (keys.isEmpty()) {
            return
        }
        remove(uploadedImagePort.findAllByImageKeyIn(keys).filter { it.memberId == memberId })
    }

    // 회원 영구 삭제. 게시물 사진과 프로필 사진이 모두 이 회원의 업로드 기록에 있다.
    // 이후 실패하면 다음 메시지에서 다시 지우고, 이미 없는 파일은 무시하므로 여러 번 실행돼도 안전하다
    @Transactional
    override fun deleteAll(memberId: Long) {
        remove(uploadedImagePort.findAllByMemberId(memberId))
    }

    @Transactional
    override fun deleteUnattached(): Int {
        val cutoff = LocalDateTime.now(clock).minus(UNATTACHED_RETENTION)
        val images = uploadedImagePort.findAllByStatusNotAndCreatedAtBefore(
            UploadStatus.ATTACHED,
            cutoff,
        )
        remove(images)
        return images.size
    }

    private fun validateStoredFile(image: UploadedImage) {
        val stored = imageStorageClient.head(image.imageKey) ?: throw CustomException(UploadStatusCode.INVALID_IMAGE)
        val valid = stored.size > 0 &&
            stored.size <= MAX_FILE_BYTES &&
            image.contentType == stored.contentType &&
            matchesSignature(image.contentType, imageStorageClient.readPrefix(image.imageKey, SIGNATURE_BYTES))
        if (!valid) {
            throw CustomException(UploadStatusCode.INVALID_IMAGE)
        }
    }

    private fun moveToPermanentKey(image: UploadedImage): AttachedImage {
        val uploadKey = image.imageKey
        val permanentKey = imageUrlResolver.permanentKeyOf(uploadKey)
        imageStorageClient.copy(uploadKey, permanentKey)
        TransactionHooks.afterCommit { imageStorageClient.delete(uploadKey) }
        TransactionHooks.afterRollback { imageStorageClient.delete(permanentKey) }
        image.attach(permanentKey)
        return AttachedImage(permanentKey, imageUrlResolver.urlOf(permanentKey))
    }

    // 파일은 커밋 뒤에 지운다. 롤백되면 DB 가 계속 가리키는 파일이 남아 있어야 하기 때문
    private fun remove(images: List<UploadedImage>) {
        if (images.isEmpty()) {
            return
        }
        uploadedImagePort.deleteAll(images)
        val keys = images.map { it.imageKey }
        TransactionHooks.afterCommit { keys.forEach { imageStorageClient.delete(it) } }
    }

    private fun matchesSignature(contentType: String, head: ByteArray): Boolean {
        return when (contentType) {
            "image/jpeg" -> head.size >= 3 && head.copyOfRange(0, 3).contentEquals(JPEG_SIGNATURE)
            "image/webp" -> ascii(head, 0, 4) == "RIFF" && ascii(head, 8, 4) == "WEBP"
            else -> false
        }
    }

    private fun ascii(content: ByteArray, offset: Int, length: Int): String {
        if (content.size < offset + length) {
            return ""
        }
        return String(content, offset, length, Charsets.US_ASCII)
    }

    companion object {
        private const val UPLOAD_KEY_PREFIX = "upload:member:"
        private const val UPLOAD_LIMIT_PER_MEMBER = 100
        private val UPLOAD_LIMIT_PERIOD = Duration.ofHours(1)
        private val UPLOAD_URL_VALIDITY = Duration.ofMinutes(10)

        // R2 수명 주기 규칙(tmp/, 1일)과 같은 기준으로 연결되지 않은 업로드 기록을 지운다
        private val UNATTACHED_RETENTION = Duration.ofDays(1)
        private const val MAX_FILE_BYTES = 10L * 1024 * 1024
        private const val SIGNATURE_BYTES = 12
        private val EXTENSIONS = mapOf(
            "image/jpeg" to "jpg",
            "image/webp" to "webp",
        )
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    }
}

package ijiri.ijiriserver.domain.upload.application.service

import ijiri.ijiriserver.domain.member.event.MemberProfileImageChangedEvent
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.domain.upload.application.data.AttachedImage
import ijiri.ijiriserver.domain.upload.application.data.request.UploadUrlRequest
import ijiri.ijiriserver.domain.upload.application.data.response.UploadResponse
import ijiri.ijiriserver.domain.upload.application.port.`in`.AttachUploadedImagesUseCase
import ijiri.ijiriserver.domain.upload.application.port.`in`.CreateUploadUrlsUseCase
import ijiri.ijiriserver.domain.upload.application.port.`in`.DeleteUnattachedUploadsUseCase
import ijiri.ijiriserver.domain.upload.application.port.`in`.DeleteUploadedImagesUseCase
import ijiri.ijiriserver.domain.upload.application.port.out.ImageStorageClient
import ijiri.ijiriserver.domain.upload.application.port.out.UploadedImagePort
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose
import ijiri.ijiriserver.domain.upload.entity.UploadStatus
import ijiri.ijiriserver.domain.upload.entity.UploadedImage
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import ijiri.ijiriserver.global.ratelimit.RateLimiter
import ijiri.ijiriserver.global.storage.ImageUrlResolver
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
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
        deleteAll(uploadedImagePort.findAllByImageKeyIn(imageKeys))
    }

    @Transactional
    override fun deleteUnattached(): Int {
        val cutoff = LocalDateTime.now(clock).minus(UNATTACHED_RETENTION)
        val images = uploadedImagePort.findAllByStatusNotAndCreatedAtBefore(
            UploadStatus.ATTACHED,
            cutoff,
        )
        deleteAll(images)
        return images.size
    }

    // 게시물을 지운 뒤 남은 업로드 기록과 파일(프로필 사진 포함)을 저장소에서 지운다.
    // 이후 롤백되면 남은 행을 다음 스케줄에 다시 지우고, 이미 없는 파일은 무시하므로 여러 번 실행돼도 안전하다
    @Order(2)
    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        deleteAll(uploadedImagePort.findAllByMemberId(event.memberId))
    }

    // 새 프로필 사진은 이 회원이 PROFILE 용도로 받은 업로드여야 한다. 아니면 예외로 프로필 변경을 롤백시킨다
    @EventListener
    @Transactional
    fun changeProfileImage(event: MemberProfileImageChangedEvent) {
        event.newImageKey?.let { attach(event.memberId, listOf(it), UploadPurpose.PROFILE) }
        // 소셜 프로필 사진처럼 직접 올리지 않은 이전 사진은 지울 파일이 없다
        imageUrlResolver.keyOf(event.previousUrl)
            ?.let { uploadedImagePort.findByImageKey(it) }
            ?.takeIf { it.memberId == event.memberId }
            ?.let { deleteAll(listOf(it)) }
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
        afterCommit { imageStorageClient.delete(uploadKey) }
        afterRollback { imageStorageClient.delete(permanentKey) }
        image.attach(permanentKey)
        return AttachedImage(permanentKey, imageUrlResolver.urlOf(permanentKey))
    }

    // 파일은 커밋 뒤에 지운다. 롤백되면 DB 가 계속 가리키는 파일이 남아 있어야 하기 때문
    private fun deleteAll(images: List<UploadedImage>) {
        uploadedImagePort.deleteAll(images)
        val keys = images.map { it.imageKey }
        afterCommit { keys.forEach { imageStorageClient.delete(it) } }
    }

    // 실패해도 이미 커밋된 결과는 바꿀 수 없으므로 로그만 남긴다 (남은 파일은 저장소에 고아로 남는다)
    private fun afterCommit(action: () -> Unit) {
        runOnCompletion(action, TransactionSynchronization.STATUS_COMMITTED)
    }

    private fun afterRollback(action: () -> Unit) {
        runOnCompletion(action, TransactionSynchronization.STATUS_ROLLED_BACK)
    }

    private fun runOnCompletion(action: () -> Unit, expectedStatus: Int) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status != expectedStatus) {
                    return
                }
                try {
                    action()
                } catch (e: RuntimeException) {
                    log.error("Storage cleanup after transaction failed", e)
                }
            }
        })
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
        private val log = LoggerFactory.getLogger(UploadService::class.java)
    }
}

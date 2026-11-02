package ijiri.ijiriserver.domain.upload.adapter.out.external

import ijiri.ijiriserver.domain.upload.application.data.StoredObject
import ijiri.ijiriserver.domain.upload.application.port.out.DbImageStoragePort
import ijiri.ijiriserver.domain.upload.application.port.out.ImageFilePort
import ijiri.ijiriserver.domain.upload.application.port.out.ImageStorageClient
import ijiri.ijiriserver.domain.upload.entity.ImageFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 로컬·테스트용: 사진을 DB(image_file)에 저장한다. 업로드 URL 은 R2 presigned URL 처럼
 * 키·Content-Type·만료 시각에 대한 HMAC 서명을 붙인 이 서버의 주소(PUT /uploads/files/{key})다.
 */
@Component
@ConditionalOnProperty(name = ["storage.type"], havingValue = "db", matchIfMissing = true)
class DbImageStorageClient(
    private val imageFilePort: ImageFilePort,
    transactionManager: PlatformTransactionManager,
    @Value("\${storage.db.server-url}") private val serverUrl: String,
    @Value("\${jwt.secret}") secret: String,
    private val clock: Clock,
) : ImageStorageClient, DbImageStoragePort {

    // 삭제는 업로드 서비스가 커밋·롤백 뒤에 부르므로, 끝난 트랜잭션에 섞이지 않게 새 트랜잭션에서 한다
    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }
    private val signingKey = (SIGNING_CONTEXT + secret).toByteArray(Charsets.UTF_8)

    override fun createUploadUrl(imageKey: String, contentType: String, validity: Duration): String {
        val expires = clock.instant().plus(validity).epochSecond
        return serverUrl + UPLOAD_PATH + imageKey +
            "?expires=" + expires +
            "&signature=" + sign(imageKey, contentType, expires)
    }

    override fun head(imageKey: String): StoredObject? {
        return imageFilePort.findByImageKey(imageKey)
            ?.let { StoredObject(it.content.size.toLong(), it.contentType) }
    }

    override fun readPrefix(imageKey: String, length: Int): ByteArray {
        return imageFilePort.findByImageKey(imageKey)
            ?.let { it.content.copyOf(minOf(length, it.content.size)) }
            ?: ByteArray(0)
    }

    override fun copy(sourceKey: String, targetKey: String) {
        imageFilePort.findByImageKey(sourceKey)
            ?.let { save(targetKey, it.contentType, it.content) }
    }

    override fun delete(imageKey: String) {
        newTransaction.executeWithoutResult { imageFilePort.deleteByImageKey(imageKey) }
    }

    override fun isValidSignature(imageKey: String, contentType: String, expires: Long, signature: String?): Boolean {
        if (expires < clock.instant().epochSecond || signature == null) {
            return false
        }
        val expected = sign(imageKey, contentType, expires).toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, signature.toByteArray(Charsets.UTF_8))
    }

    override fun save(imageKey: String, contentType: String, content: ByteArray) {
        imageFilePort.save(
            ImageFile(
                imageKey = imageKey,
                contentType = contentType,
                content = content,
            )
        )
    }

    override fun load(imageKey: String): ImageFile? {
        return imageFilePort.findByImageKey(imageKey)
    }

    private fun sign(imageKey: String, contentType: String, expires: Long): String {
        try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(signingKey, HMAC_ALGORITHM))
            val digest = mac.doFinal("$imageKey\n$contentType\n$expires".toByteArray(Charsets.UTF_8))
            return HexFormat.of().formatHex(digest)
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException(e)
        }
    }

    companion object {
        private const val UPLOAD_PATH = "/uploads/files/"
        private const val HMAC_ALGORITHM = "HmacSHA256"

        // JWT 와 같은 비밀값을 쓰되, 용도가 다른 서명이 서로 통하지 않도록 앞에 붙인다
        private const val SIGNING_CONTEXT = "image-upload:"
    }
}

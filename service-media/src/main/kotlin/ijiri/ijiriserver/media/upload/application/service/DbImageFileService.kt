package ijiri.ijiriserver.media.upload.application.service

import ijiri.ijiriserver.media.upload.application.data.ImageFileContent
import ijiri.ijiriserver.media.upload.application.port.`in`.LoadImageFileUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.ReceiveImageFileUseCase
import ijiri.ijiriserver.media.upload.application.port.out.DbImageStoragePort
import ijiri.ijiriserver.media.upload.application.port.out.UploadedImagePort
import ijiri.ijiriserver.media.upload.entity.UploadStatus
import ijiri.ijiriserver.media.upload.exception.UploadStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(name = ["storage.type"], havingValue = "db", matchIfMissing = true)
class DbImageFileService(
    private val uploadedImagePort: UploadedImagePort,
    private val dbImageStoragePort: DbImageStoragePort,
) : ReceiveImageFileUseCase,
    LoadImageFileUseCase {

    // R2 처럼 서명된 키·Content-Type 으로 한 번만 받는다. 이미지인지는 게시물에 연결할 때 확인한다
    @Transactional
    override fun receive(imageKey: String, expires: Long, signature: String, contentType: String, content: ByteArray) {
        val image = uploadedImagePort.findByImageKey(imageKey)
            ?.takeIf { it.status == UploadStatus.PENDING }
            ?.takeIf { it.contentType == contentType }
            ?.takeIf { dbImageStoragePort.isValidSignature(imageKey, contentType, expires, signature) }
            ?: throw CustomException(UploadStatusCode.INVALID_UPLOAD_URL)
        dbImageStoragePort.save(imageKey, contentType, content)
        image.markUploaded()
    }

    @Transactional(readOnly = true)
    override fun load(imageKey: String): ImageFileContent {
        val file = dbImageStoragePort.load(imageKey) ?: throw CustomException(UploadStatusCode.IMAGE_NOT_FOUND)
        return ImageFileContent(file.contentType, file.content)
    }
}

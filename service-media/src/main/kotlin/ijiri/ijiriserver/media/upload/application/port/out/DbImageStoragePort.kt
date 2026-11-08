package ijiri.ijiriserver.media.upload.application.port.out

import ijiri.ijiriserver.media.upload.entity.ImageFile

interface DbImageStoragePort {

    fun isValidSignature(imageKey: String, contentType: String, expires: Long, signature: String?): Boolean

    fun save(imageKey: String, contentType: String, content: ByteArray)

    fun load(imageKey: String): ImageFile?
}

package ijiri.ijiriserver.media.upload.application.port.out

import ijiri.ijiriserver.media.upload.entity.ImageFile

interface ImageFilePort {

    fun findByImageKey(imageKey: String): ImageFile?

    fun deleteByImageKey(imageKey: String)

    fun save(entity: ImageFile): ImageFile
}

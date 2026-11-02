package ijiri.ijiriserver.domain.upload.application.port.out

import ijiri.ijiriserver.domain.upload.entity.ImageFile

interface ImageFilePort {

    fun findByImageKey(imageKey: String): ImageFile?

    fun deleteByImageKey(imageKey: String)

    fun save(entity: ImageFile): ImageFile
}

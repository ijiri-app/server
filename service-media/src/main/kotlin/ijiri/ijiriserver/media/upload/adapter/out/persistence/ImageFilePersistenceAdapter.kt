package ijiri.ijiriserver.media.upload.adapter.out.persistence

import ijiri.ijiriserver.media.upload.application.port.out.ImageFilePort
import ijiri.ijiriserver.media.upload.entity.ImageFile
import org.springframework.stereotype.Component

@Component
class ImageFilePersistenceAdapter(
    private val imageFileJpaRepository: ImageFileJpaRepository,
) : ImageFilePort {

    override fun findByImageKey(imageKey: String): ImageFile? {
        return imageFileJpaRepository.findByImageKey(imageKey)
    }

    override fun deleteByImageKey(imageKey: String) {
        imageFileJpaRepository.deleteByImageKey(imageKey)
    }

    override fun save(entity: ImageFile): ImageFile {
        return imageFileJpaRepository.save(entity)
    }
}

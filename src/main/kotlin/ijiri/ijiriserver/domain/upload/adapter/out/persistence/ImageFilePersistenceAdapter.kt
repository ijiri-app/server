package ijiri.ijiriserver.domain.upload.adapter.out.persistence

import ijiri.ijiriserver.domain.upload.application.port.out.ImageFilePort
import ijiri.ijiriserver.domain.upload.entity.ImageFile
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

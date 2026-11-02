package ijiri.ijiriserver.domain.upload.adapter.out.persistence

import ijiri.ijiriserver.domain.upload.application.port.out.UploadedImagePort
import ijiri.ijiriserver.domain.upload.entity.UploadStatus
import ijiri.ijiriserver.domain.upload.entity.UploadedImage
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UploadedImagePersistenceAdapter(
    private val uploadedImageJpaRepository: UploadedImageJpaRepository,
) : UploadedImagePort {

    override fun findByImageKey(imageKey: String): UploadedImage? {
        return uploadedImageJpaRepository.findByImageKey(imageKey)
    }

    override fun findAllByImageKeyIn(imageKeys: Collection<String>): List<UploadedImage> {
        return uploadedImageJpaRepository.findAllByImageKeyIn(imageKeys)
    }

    override fun findAllByMemberId(memberId: Long): List<UploadedImage> {
        return uploadedImageJpaRepository.findAllByMemberId(memberId)
    }

    override fun findAllByStatusNotAndCreatedAtBefore(
        status: UploadStatus,
        cutoff: LocalDateTime,
    ): List<UploadedImage> {
        return uploadedImageJpaRepository.findAllByStatusNotAndCreatedAtBefore(status, cutoff)
    }

    override fun deleteAll(entities: Iterable<UploadedImage>) {
        uploadedImageJpaRepository.deleteAll(entities)
    }

    override fun save(entity: UploadedImage): UploadedImage {
        return uploadedImageJpaRepository.save(entity)
    }
}

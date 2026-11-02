package ijiri.ijiriserver.media.upload.adapter.out.persistence

import ijiri.ijiriserver.media.upload.entity.UploadStatus
import ijiri.ijiriserver.media.upload.entity.UploadedImage
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface UploadedImageJpaRepository : JpaRepository<UploadedImage, Long> {

    fun findByImageKey(imageKey: String): UploadedImage?

    fun findAllByImageKeyIn(imageKeys: Collection<String>): List<UploadedImage>

    fun findAllByMemberId(memberId: Long): List<UploadedImage>

    fun findAllByStatusNotAndCreatedAtBefore(status: UploadStatus, cutoff: LocalDateTime): List<UploadedImage>
}

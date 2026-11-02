package ijiri.ijiriserver.domain.upload.application.port.out

import ijiri.ijiriserver.domain.upload.entity.UploadStatus
import ijiri.ijiriserver.domain.upload.entity.UploadedImage
import java.time.LocalDateTime

interface UploadedImagePort {

    fun findByImageKey(imageKey: String): UploadedImage?

    fun findAllByImageKeyIn(imageKeys: Collection<String>): List<UploadedImage>

    fun findAllByMemberId(memberId: Long): List<UploadedImage>

    fun findAllByStatusNotAndCreatedAtBefore(status: UploadStatus, cutoff: LocalDateTime): List<UploadedImage>

    fun deleteAll(entities: Iterable<UploadedImage>)

    fun save(entity: UploadedImage): UploadedImage
}

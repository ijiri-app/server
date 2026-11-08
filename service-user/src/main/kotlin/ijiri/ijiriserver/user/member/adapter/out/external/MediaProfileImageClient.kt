package ijiri.ijiriserver.user.member.adapter.out.external

import ijiri.ijiriserver.common.contract.AttachImagesRequest
import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.DeleteImagesRequest
import ijiri.ijiriserver.common.contract.UploadPurpose
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.user.member.application.port.out.ProfileImagePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MediaProfileImageClient(
    factory: InternalClientFactory,
    @Value("\${services.media.url}") mediaUrl: String,
) : ProfileImagePort {

    private val client = factory.forService(mediaUrl)

    override fun attach(memberId: Long, imageKey: String): AttachedImage =
        client.post<List<AttachedImage>>(
            "/internal/images/attach",
            AttachImagesRequest(memberId, listOf(imageKey), UploadPurpose.PROFILE),
        ).first()

    override fun delete(memberId: Long, imageUrl: String) {
        client.postWithoutResult("/internal/images/delete", DeleteImagesRequest(memberId, listOf(imageUrl)))
    }
}

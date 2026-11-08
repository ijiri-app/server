package ijiri.ijiriserver.community.post.adapter.out.external

import ijiri.ijiriserver.common.contract.AttachImagesRequest
import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.DeleteImagesRequest
import ijiri.ijiriserver.common.contract.UploadPurpose
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.community.post.application.port.out.ImageAttacher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MediaServiceClient(
    factory: InternalClientFactory,
    @Value("\${services.media.url}") mediaUrl: String,
) : ImageAttacher {

    private val client = factory.forService(mediaUrl)

    override fun attach(memberId: Long, imageKeys: List<String>): List<AttachedImage> =
        client.post("/internal/images/attach", AttachImagesRequest(memberId, imageKeys, UploadPurpose.POST))

    override fun delete(memberId: Long, imageKeys: List<String>) {
        client.postWithoutResult("/internal/images/delete", DeleteImagesRequest(memberId, imageKeys = imageKeys))
    }
}

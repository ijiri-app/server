package ijiri.ijiriserver.user.ownedcar.adapter.out.external

import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.user.ownedcar.application.port.out.OwnedCarPostCounter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CommunityPostCountClient(
    factory: InternalClientFactory,
    @Value("\${services.community.url}") communityUrl: String,
) : OwnedCarPostCounter {

    private val client = factory.forService(communityUrl)

    override fun countPosts(ownedCarIds: Collection<Long>): Map<Long, Long> {
        if (ownedCarIds.isEmpty()) {
            return emptyMap()
        }
        return client.get("/internal/owned-cars/post-counts?ids={ids}", ownedCarIds.joinToString(","))
    }
}

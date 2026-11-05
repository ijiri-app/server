package ijiri.ijiriserver.user.member.adapter.out.external

import ijiri.ijiriserver.common.contract.MemberCounts
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.user.member.application.port.out.MemberCountReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CommunityMemberCountClient(
    factory: InternalClientFactory,
    @Value("\${services.community.url}") communityUrl: String,
) : MemberCountReader {

    private val client = factory.forService(communityUrl)

    override fun countsOf(memberId: Long): MemberCounts = client.get("/internal/members/{id}/counts", memberId)
}

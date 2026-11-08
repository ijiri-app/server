package ijiri.ijiriserver.community.post.adapter.out.external

import ijiri.ijiriserver.common.contract.MemberSummary
import ijiri.ijiriserver.common.contract.OwnedCarSnapshot
import ijiri.ijiriserver.common.contract.SuspendMemberRequest
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.community.post.application.port.out.InterestCarReader
import ijiri.ijiriserver.community.post.application.port.out.MemberReader
import ijiri.ijiriserver.community.post.application.port.out.OwnedCarReader
import ijiri.ijiriserver.community.report.application.port.out.MemberSuspender
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * service-user 의 내부 API. 회원, 보유 차량, 관심 차종을 읽고 이용 정지를 요청한다.
 */
@Component
class UserServiceClient(
    factory: InternalClientFactory,
    @Value("\${services.user.url}") userUrl: String,
) : MemberReader,
    OwnedCarReader,
    InterestCarReader,
    MemberSuspender {

    private val client = factory.forService(userUrl)

    // 탈퇴했거나 없는 회원은 NOT_FOUND 로 오므로 null 로 바꾼다
    override fun findActive(memberId: Long): MemberSummary? {
        try {
            return client.get("/internal/members/{id}", memberId)
        } catch (e: CustomException) {
            if (e.statusCode.httpStatus == HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    override fun getActive(memberIds: Collection<Long>): List<MemberSummary> {
        if (memberIds.isEmpty()) {
            return listOf()
        }
        return client.get("/internal/members?ids={ids}", memberIds.joinToString(","))
    }

    override fun getSnapshot(memberId: Long, ownedCarId: Long): OwnedCarSnapshot =
        client.get("/internal/owned-cars/{id}?memberId={memberId}", ownedCarId, memberId)

    override fun getCarModelIds(memberId: Long): List<Long> =
        client.get("/internal/members/{id}/interest-cars", memberId)

    override fun suspend(memberId: Long, until: LocalDateTime) {
        client.postWithoutResult("/internal/members/{id}/suspend", SuspendMemberRequest(until), memberId)
    }
}

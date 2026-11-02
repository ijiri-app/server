package ijiri.ijiriserver.user.member.application.service

import ijiri.ijiriserver.common.contract.MemberSummary
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse
import ijiri.ijiriserver.user.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberProfileUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberSummaryUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMyProfileUseCase
import ijiri.ijiriserver.user.member.application.port.out.MemberCountReader
import ijiri.ijiriserver.user.member.application.port.out.MemberPort
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.exception.MemberStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberPort: MemberPort,
    private val memberCountReader: MemberCountReader,
) : GetMyProfileUseCase,
    GetMemberProfileUseCase,
    GetMemberUseCase,
    FindActiveMemberUseCase,
    FindEmailMemberUseCase,
    GetMemberSummaryUseCase {

    override fun getMe(memberId: Long): MemberResponse {
        val member = getById(memberId)
        val counts = memberCountReader.countsOf(memberId)
        return MemberResponse.me(member, counts.postCount, counts.receivedWishCount)
    }

    override fun getProfile(memberId: Long): MemberResponse {
        val member = getById(memberId)
        val counts = memberCountReader.countsOf(memberId)
        return MemberResponse.profile(member, counts.postCount, counts.receivedWishCount)
    }

    override fun getById(memberId: Long): Member {
        return findActiveMember(memberId)
            ?: throw CustomException(MemberStatusCode.MEMBER_NOT_FOUND)
    }

    override fun findActiveMember(memberId: Long): Member? {
        return memberPort.findByIdAndDeletedAtIsNull(memberId)
    }

    override fun findEmailMember(email: String): Member? {
        return memberPort.findByProviderAndProviderMemberId(Provider.EMAIL, email)
    }

    override fun getSummary(memberId: Long): MemberSummary = toSummary(getById(memberId))

    override fun getSummaries(memberIds: Collection<Long>): List<MemberSummary> {
        if (memberIds.isEmpty()) {
            return listOf()
        }
        return memberPort.findAllByIdInAndDeletedAtIsNull(memberIds).map(::toSummary)
    }

    private fun toSummary(member: Member): MemberSummary =
        MemberSummary(member.id, member.nickname, member.profileImageUrl, member.statusMessage)
}

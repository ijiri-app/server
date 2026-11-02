package ijiri.ijiriserver.domain.member.application.service

import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse
import ijiri.ijiriserver.domain.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetActiveMembersUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberProfileUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMyProfileUseCase
import ijiri.ijiriserver.domain.member.application.port.out.MemberPort
import ijiri.ijiriserver.domain.member.application.port.out.MemberPostCounter
import ijiri.ijiriserver.domain.member.application.port.out.MemberWishCounter
import ijiri.ijiriserver.domain.member.entity.Member
import ijiri.ijiriserver.domain.member.entity.Provider
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import ijiri.ijiriserver.global.security.AdminVerifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberPort: MemberPort,
    private val memberPostCounter: MemberPostCounter,
    private val memberWishCounter: MemberWishCounter,
) : GetMyProfileUseCase,
    GetMemberProfileUseCase,
    GetMemberUseCase,
    FindActiveMemberUseCase,
    GetActiveMembersUseCase,
    FindEmailMemberUseCase,
    AdminVerifier {

    override fun getMe(memberId: Long): MemberResponse {
        val member = getById(memberId)
        return MemberResponse.me(
            member,
            memberPostCounter.countVisiblePosts(memberId),
            memberWishCounter.countReceivedWishes(memberId),
        )
    }

    override fun getProfile(memberId: Long): MemberResponse {
        val member = getById(memberId)
        return MemberResponse.profile(
            member,
            memberPostCounter.countVisiblePosts(memberId),
            memberWishCounter.countReceivedWishes(memberId),
        )
    }

    override fun getById(memberId: Long): Member {
        return findActiveMember(memberId)
            ?: throw CustomException(MemberStatusCode.MEMBER_NOT_FOUND)
    }

    override fun findActiveMember(memberId: Long): Member? {
        return memberPort.findByIdAndDeletedAtIsNull(memberId)
    }

    override fun getActiveMembers(memberIds: Collection<Long>): List<Member> {
        return memberPort.findAllByIdInAndDeletedAtIsNull(memberIds)
    }

    override fun findEmailMember(email: String): Member? {
        return memberPort.findByProviderAndProviderMemberId(Provider.EMAIL, email)
    }

    // 역할을 바꾸면 다시 로그인하지 않아도 바로 반영되도록 관리자 API 요청마다 DB 를 본다
    override fun isAdmin(memberId: String): Boolean {
        return findActiveMember(memberId.toLong())?.isAdmin() ?: false
    }
}

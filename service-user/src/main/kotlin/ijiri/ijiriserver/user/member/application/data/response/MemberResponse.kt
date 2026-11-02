package ijiri.ijiriserver.user.member.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider

/**
 * member 도메인의 모든 API 응답.
 * 로그인 응답의 member 는 summary, 내 정보는 me, 다른 회원 프로필은 profile(email·provider 제외).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MemberResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val statusMessage: String?,
    val provider: Provider?,
    val email: String?,
    val postCount: Long?,
    val receivedWishCount: Long?,
) {

    companion object {
        fun summary(member: Member): MemberResponse {
            return MemberResponse(
                member.id,
                member.nickname,
                member.profileImageUrl,
                null,
                null,
                null,
                null,
                null,
            )
        }

        fun me(member: Member, postCount: Long, receivedWishCount: Long): MemberResponse {
            return MemberResponse(
                member.id,
                member.nickname,
                member.profileImageUrl,
                member.statusMessage,
                member.provider,
                member.email,
                postCount,
                receivedWishCount,
            )
        }

        fun profile(member: Member, postCount: Long, receivedWishCount: Long): MemberResponse {
            return MemberResponse(
                member.id,
                member.nickname,
                member.profileImageUrl,
                member.statusMessage,
                null,
                null,
                postCount,
                receivedWishCount,
            )
        }
    }
}

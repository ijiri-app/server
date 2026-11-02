package ijiri.ijiriserver.domain.member.application.data

import ijiri.ijiriserver.domain.member.entity.Provider

/**
 * 소셜 서버에서 검증 후 꺼낸 회원 정보. email/nickname/profileImageUrl 은 선택 동의라 null 일 수 있다.
 */
data class MemberRegisterCommand(
    val provider: Provider,
    val providerMemberId: String,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
)

package ijiri.ijiriserver.user.auth.token.application.data

import ijiri.ijiriserver.user.member.entity.Role

/**
 * 게이트웨이 검증 결과. 역할은 토큰이 아니라 DB 의 현재 값이라 바꾸면 다시 로그인하지 않아도 바로 반영된다.
 */
data class VerifiedMember(
    val memberId: Long,
    val role: Role,
)

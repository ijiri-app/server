package ijiri.ijiriserver.common.contract

/**
 * service-user 의 GET /internal/members 응답. 활성(탈퇴하지 않은) 회원만 돌려준다.
 */
data class MemberSummary(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val statusMessage: String?,
)

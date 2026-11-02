package ijiri.ijiriserver.common.contract

/**
 * service-community 의 GET /internal/members/{id}/counts 응답. 마이 페이지와 프로필의 게시물 수, 받은 담기 수.
 */
data class MemberCounts(
    val postCount: Long,
    val receivedWishCount: Long,
)

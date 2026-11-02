package ijiri.ijiriserver.domain.block.application.data.response

/**
 * block 도메인의 모든 API 응답 (차단 목록). 차단·해제는 204 라 본문이 없다.
 */
data class BlockResponse(
    val items: List<BlockedMember>,
) {

    data class BlockedMember(
        val memberId: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )
}

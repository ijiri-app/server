package ijiri.ijiriserver.common.messaging

/**
 * 회원 탈퇴(soft delete). service-community 가 받아 게시물과 담기를 숨긴다. 여러 번 받아도 같은 결과다.
 */
data class MemberWithdrawnMessage(
    val memberId: Long,
)

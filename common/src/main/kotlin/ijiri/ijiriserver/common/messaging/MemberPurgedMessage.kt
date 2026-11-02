package ijiri.ijiriserver.common.messaging

/**
 * 탈퇴 후 보관 기간이 지나 회원 행을 완전히 삭제한다. service-community 는 게시물·담기·차단·신고를,
 * service-media 는 이 회원의 업로드 기록과 파일을 지운다. 소비자는 여러 번 받아도 안전하게(멱등) 처리한다.
 */
data class MemberPurgedMessage(
    val memberId: Long,
)

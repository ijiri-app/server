package ijiri.ijiriserver.common.contract

import java.time.LocalDateTime

/**
 * POST /internal/members/{id}/suspend. 신고 처리가 회원을 이용 정지할 때 보낸다.
 */
data class SuspendMemberRequest(
    val until: LocalDateTime,
)

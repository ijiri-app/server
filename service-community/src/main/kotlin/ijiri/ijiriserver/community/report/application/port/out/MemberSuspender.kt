package ijiri.ijiriserver.community.report.application.port.out

import java.time.LocalDateTime

/**
 * 이용 정지는 service-user 가 한다 (세션도 끊는다). 이미 탈퇴한 회원이면 아무것도 하지 않는다.
 */
interface MemberSuspender {

    fun suspend(memberId: Long, until: LocalDateTime)
}

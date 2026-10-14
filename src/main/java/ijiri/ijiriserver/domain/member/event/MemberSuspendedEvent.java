package ijiri.ijiriserver.domain.member.event;

/**
 * 관리자가 회원을 이용 정지한 순간 발행. 토큰 도메인이 받아서 세션을 끊는다.
 */
public record MemberSuspendedEvent(
        Long memberId
) {
}

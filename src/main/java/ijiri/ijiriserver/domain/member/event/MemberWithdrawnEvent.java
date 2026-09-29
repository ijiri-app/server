package ijiri.ijiriserver.domain.member.event;

/**
 * 회원 탈퇴 시 발행. 회원 데이터를 가진 다른 도메인(토큰, 관심 차종)이 받아서 자기 데이터를 지운다.
 * member 가 다른 도메인 서비스를 직접 호출하지 않으므로 서비스 간 순환 의존이 생기지 않는다.
 */
public record MemberWithdrawnEvent(
        Long memberId
) {
}

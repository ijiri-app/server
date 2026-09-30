package ijiri.ijiriserver.domain.member.event;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 회원 탈퇴(soft delete) 순간 발행. 보관 기간을 기다리지 않고 즉시 처리할 일을 각 도메인이 받아서 한다.
 * (refresh token 폐기, 카카오 연결 끊기, 게시물 도메인이 생기면 게시물 숨기기)
 * member 가 다른 도메인 서비스를 직접 호출하지 않으므로 도메인 간 순환 의존이 생기지 않는다.
 */
public record MemberWithdrawnEvent(
        Long memberId,
        Provider provider,
        String providerMemberId
) {
}

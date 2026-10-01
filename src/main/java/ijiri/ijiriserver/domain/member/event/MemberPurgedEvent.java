package ijiri.ijiriserver.domain.member.event;

import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * 탈퇴 후 보관 기간이 지나 회원 행을 완전히 삭제하기 직전에 발행. 회원 데이터를 가진 도메인이 받아서 자기 데이터를 지운다.
 * (관심 차종, 보유 차량, 게시물, 업로드 사진과 파일, 위시리스트, 차단, 신고, 실패한 소셜 연결 끊기 재시도)
 * 같은 트랜잭션에서 실행되므로 리스너가 실패하면 회원 행 삭제도 롤백되고 다음 스케줄에 다시 시도된다.
 * 따라서 리스너는 여러 번 실행돼도 안전하게(멱등) 만든다.
 */
public record MemberPurgedEvent(
        Long memberId,
        Provider provider,
        String providerMemberId
) {
}

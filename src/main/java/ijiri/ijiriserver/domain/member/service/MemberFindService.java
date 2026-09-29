package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.entity.Member;

/**
 * 다른 도메인(토큰 재발급 등)이 회원 엔티티를 조회할 때 사용한다.
 */
public interface MemberFindService {

    Member getById(Long memberId);
}

package ijiri.ijiriserver.domain.member.service;

/**
 * 마이 페이지의 게시물 수. member 가 게시물 도메인에 의존하지 않도록 게시물 도메인이 구현한다.
 */
public interface MemberPostCounter {

    long countVisiblePosts(Long memberId);
}

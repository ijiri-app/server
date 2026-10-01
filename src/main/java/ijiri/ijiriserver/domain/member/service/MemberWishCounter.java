package ijiri.ijiriserver.domain.member.service;

/**
 * 내 게시물 부품이 받은 담기 수(본인 담기 제외). member 가 위시리스트 도메인에 의존하지 않도록 위시리스트 도메인이 구현한다.
 */
public interface MemberWishCounter {

    long countReceivedWishes(Long memberId);
}

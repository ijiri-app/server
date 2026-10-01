package ijiri.ijiriserver.domain.member.event;

/**
 * 프로필 사진을 바꾸거나 지운 순간 발행. 업로드 도메인이 받아서 새 사진이 이 회원이 올린 것인지 확인하고
 * 이전 사진 파일을 지운다. 같은 트랜잭션에서 실행되므로 리스너가 거부하면(예외) 프로필 변경도 롤백된다.
 * previousUrl, newImageKey 는 null 일 수 있다 (없던 사진 추가, 사진 삭제, 소셜 프로필 사진 등).
 */
public record MemberProfileImageChangedEvent(
        Long memberId,
        String previousUrl,
        String newImageKey
) {
}

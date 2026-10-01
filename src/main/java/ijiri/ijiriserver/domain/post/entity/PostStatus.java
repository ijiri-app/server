package ijiri.ijiriserver.domain.post.entity;

public enum PostStatus {
    PUBLIC,
    // 관리자가 신고를 처리하며 숨김
    HIDDEN,
    // 작성자가 탈퇴해 비공개. 30일 뒤 회원 영구 삭제 때 함께 삭제된다
    AUTHOR_WITHDRAWN
}

package ijiri.ijiriserver.common.messaging

/**
 * 게시물 삭제(작성자 삭제, 회원 영구 삭제). service-media 가 사진 기록과 파일을 지운다.
 */
data class PostDeletedMessage(
    val postId: Long,
    val imageKeys: List<String>,
)

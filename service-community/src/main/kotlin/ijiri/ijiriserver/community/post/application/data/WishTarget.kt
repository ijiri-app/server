package ijiri.ijiriserver.community.post.application.data

/**
 * 위시리스트에 담을 수 있는(공개 게시물의) 부품.
 */
data class WishTarget(
    val postPartId: Long,
    val postId: Long,
    val postAuthorId: Long,
)

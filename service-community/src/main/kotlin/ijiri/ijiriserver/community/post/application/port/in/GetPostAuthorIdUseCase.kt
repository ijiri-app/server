package ijiri.ijiriserver.community.post.application.port.`in`

interface GetPostAuthorIdUseCase {

    /**
     * 신고 대상 확인용. 없는 게시물이면 NOT_FOUND.
     */
    fun getAuthorId(postId: Long): Long
}

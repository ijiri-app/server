package ijiri.ijiriserver.community.post.application.port.`in`

interface HidePostUseCase {

    /**
     * 이미 삭제된 게시물이면 아무것도 하지 않는다 (신고 처리가 막히지 않도록).
     */
    fun hide(postId: Long)
}

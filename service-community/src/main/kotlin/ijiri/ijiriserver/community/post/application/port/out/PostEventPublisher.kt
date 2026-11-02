package ijiri.ijiriserver.community.post.application.port.out

/**
 * 다른 서비스에 알리는 게시물 이벤트. 커밋 뒤에 부르므로 실패해도 응답은 바뀌지 않고 로그만 남는다.
 */
interface PostEventPublisher {

    fun postDeleted(postId: Long, imageKeys: List<String>)

    fun partUsageChanged(carModelId: Long, addedPartIds: Set<Long>, removedPartIds: Set<Long>)
}

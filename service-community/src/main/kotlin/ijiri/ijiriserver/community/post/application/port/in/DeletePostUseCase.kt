package ijiri.ijiriserver.community.post.application.port.`in`

interface DeletePostUseCase {

    fun delete(memberId: Long, postId: Long)
}

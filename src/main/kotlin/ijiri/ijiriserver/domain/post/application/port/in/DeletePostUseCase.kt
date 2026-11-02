package ijiri.ijiriserver.domain.post.application.port.`in`

interface DeletePostUseCase {

    fun delete(memberId: Long, postId: Long)
}

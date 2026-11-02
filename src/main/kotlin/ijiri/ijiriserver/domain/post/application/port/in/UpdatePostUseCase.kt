package ijiri.ijiriserver.domain.post.application.port.`in`

import ijiri.ijiriserver.domain.post.application.data.request.PostUpdateRequest

interface UpdatePostUseCase {

    fun update(memberId: Long, postId: Long, request: PostUpdateRequest)
}

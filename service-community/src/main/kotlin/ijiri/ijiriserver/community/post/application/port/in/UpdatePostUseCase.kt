package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.request.PostUpdateRequest

interface UpdatePostUseCase {

    fun update(memberId: Long, postId: Long, request: PostUpdateRequest)
}

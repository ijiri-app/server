package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.request.PostCreateRequest
import ijiri.ijiriserver.community.post.application.data.response.PostResponse

interface CreatePostUseCase {

    fun create(memberId: Long, request: PostCreateRequest): PostResponse
}

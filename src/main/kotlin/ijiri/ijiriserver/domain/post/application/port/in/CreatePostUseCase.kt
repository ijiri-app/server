package ijiri.ijiriserver.domain.post.application.port.`in`

import ijiri.ijiriserver.domain.post.application.data.request.PostCreateRequest
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse

interface CreatePostUseCase {

    fun create(memberId: Long, request: PostCreateRequest): PostResponse
}

package ijiri.ijiriserver.domain.post.application.port.`in`

import ijiri.ijiriserver.domain.post.application.data.response.PostResponse

interface GetMemberPostsUseCase {

    fun getMemberPosts(viewerId: Long?, memberId: Long, cursor: Long?, size: Int): PostResponse
}

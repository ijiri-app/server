package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.response.PostResponse

interface GetMemberPostsUseCase {

    fun getMemberPosts(viewerId: Long?, memberId: Long, cursor: Long?, size: Int): PostResponse
}

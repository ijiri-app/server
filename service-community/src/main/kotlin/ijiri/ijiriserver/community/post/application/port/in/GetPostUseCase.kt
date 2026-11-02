package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.response.PostResponse

interface GetPostUseCase {

    /**
     * viewerId 는 비로그인이면 null.
     */
    fun getPost(viewerId: Long?, postId: Long): PostResponse
}

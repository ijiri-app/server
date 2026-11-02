package ijiri.ijiriserver.community.post.application.port.`in`

import ijiri.ijiriserver.community.post.application.data.WishTarget

interface GetWishTargetUseCase {

    /**
     * 공개 게시물의 부품이 아니면 NOT_FOUND.
     */
    fun getWishTarget(postPartId: Long): WishTarget
}

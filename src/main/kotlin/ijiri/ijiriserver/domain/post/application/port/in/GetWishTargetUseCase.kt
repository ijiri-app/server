package ijiri.ijiriserver.domain.post.application.port.`in`

import ijiri.ijiriserver.domain.post.application.data.WishTarget

interface GetWishTargetUseCase {

    /**
     * 공개 게시물의 부품이 아니면 NOT_FOUND.
     */
    fun getWishTarget(postPartId: Long): WishTarget
}

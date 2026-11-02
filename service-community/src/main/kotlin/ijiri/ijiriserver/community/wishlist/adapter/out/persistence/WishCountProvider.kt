package ijiri.ijiriserver.community.wishlist.adapter.out.persistence

import ijiri.ijiriserver.community.post.application.port.out.PostWishReader
import ijiri.ijiriserver.community.wishlist.application.port.out.WishlistItemPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 게시물 도메인에 담기 수를 제공한다. 저장소만 의존해 서비스 사이에 순환 의존이 생기지 않게 한다.
 */
@Component
@Transactional(readOnly = true)
class WishCountProvider(
    private val wishlistItemPort: WishlistItemPort,
) : PostWishReader {

    override fun countWishes(postPartIds: Collection<Long>): Map<Long, Long> {
        if (postPartIds.isEmpty()) {
            return emptyMap()
        }
        return wishlistItemPort.countByPostPartIds(postPartIds)
            .associate { row -> row[0] as Long to row[1] as Long }
    }

    override fun findWishedPostPartIds(memberId: Long, postPartIds: Collection<Long>): Set<Long> {
        if (postPartIds.isEmpty()) {
            return emptySet()
        }
        return wishlistItemPort.findPostPartIds(memberId, postPartIds).toSet()
    }
}

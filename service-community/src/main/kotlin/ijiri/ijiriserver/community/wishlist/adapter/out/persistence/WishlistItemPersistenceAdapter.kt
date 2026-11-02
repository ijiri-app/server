package ijiri.ijiriserver.community.wishlist.adapter.out.persistence

import ijiri.ijiriserver.community.wishlist.application.port.out.WishlistItemPort
import ijiri.ijiriserver.community.wishlist.entity.WishlistItem
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component

@Component
class WishlistItemPersistenceAdapter(
    private val wishlistItemJpaRepository: WishlistItemJpaRepository,
) : WishlistItemPort {

    override fun findByMemberIdAndPostPartId(memberId: Long, postPartId: Long): WishlistItem? =
        wishlistItemJpaRepository.findByMemberIdAndPostPartId(memberId, postPartId)

    override fun findByIdAndMemberId(id: Long, memberId: Long): WishlistItem? =
        wishlistItemJpaRepository.findByIdAndMemberId(id, memberId)

    override fun findByMemberIdAndIdLessThanOrderByIdDesc(
        memberId: Long,
        cursor: Long,
        limit: Limit,
    ): List<WishlistItem> = wishlistItemJpaRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursor, limit)

    override fun countByPostPartIds(postPartIds: Collection<Long>): List<Array<Any>> =
        wishlistItemJpaRepository.countByPostPartIds(postPartIds)

    override fun countReceived(memberId: Long): Long = wishlistItemJpaRepository.countReceived(memberId)

    override fun findPostPartIds(memberId: Long, postPartIds: Collection<Long>): List<Long> =
        wishlistItemJpaRepository.findPostPartIds(memberId, postPartIds)

    override fun hideAllByMemberId(memberId: Long) {
        wishlistItemJpaRepository.hideAllByMemberId(memberId)
    }

    override fun deleteAllByMemberIdInBulk(memberId: Long) {
        wishlistItemJpaRepository.deleteAllByMemberIdInBulk(memberId)
    }

    override fun deleteAllByPostId(postId: Long) {
        wishlistItemJpaRepository.deleteAllByPostId(postId)
    }

    override fun deleteAllByPostPartIds(postPartIds: Collection<Long>) {
        wishlistItemJpaRepository.deleteAllByPostPartIds(postPartIds)
    }

    override fun delete(entity: WishlistItem) {
        wishlistItemJpaRepository.delete(entity)
    }

    override fun save(entity: WishlistItem): WishlistItem = wishlistItemJpaRepository.save(entity)
}

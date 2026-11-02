package ijiri.ijiriserver.domain.wishlist.application.port.out

import ijiri.ijiriserver.domain.wishlist.entity.WishlistItem
import org.springframework.data.domain.Limit

interface WishlistItemPort {

    fun findByMemberIdAndPostPartId(memberId: Long, postPartId: Long): WishlistItem?

    fun findByIdAndMemberId(id: Long, memberId: Long): WishlistItem?

    fun findByMemberIdAndIdLessThanOrderByIdDesc(memberId: Long, cursor: Long, limit: Limit): List<WishlistItem>

    fun countByPostPartIds(postPartIds: Collection<Long>): List<Array<Any>>

    fun countReceived(memberId: Long): Long

    fun findPostPartIds(memberId: Long, postPartIds: Collection<Long>): List<Long>

    fun hideAllByMemberId(memberId: Long)

    fun deleteAllByMemberIdInBulk(memberId: Long)

    fun deleteAllByPostId(postId: Long)

    fun deleteAllByPostPartIds(postPartIds: Collection<Long>)

    fun delete(entity: WishlistItem)

    fun save(entity: WishlistItem): WishlistItem
}

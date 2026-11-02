package ijiri.ijiriserver.domain.post.application.port.out

import ijiri.ijiriserver.domain.post.entity.Post
import ijiri.ijiriserver.domain.post.entity.PostStatus
import org.springframework.data.jpa.domain.Specification

interface PostPort {

    fun findAllByMemberId(memberId: Long): List<Post>

    fun countByMemberIdAndStatus(memberId: Long, status: PostStatus): Long

    fun countByOwnedCarIds(ownedCarIds: Collection<Long>): List<Array<Any>>

    fun hideAllByAuthor(memberId: Long)

    fun delete(entity: Post)

    fun findById(id: Long): Post?

    fun save(entity: Post): Post

    fun flush()

    fun findNewest(spec: Specification<Post>, limit: Int): List<Post>
}

package ijiri.ijiriserver.community.post.adapter.out.persistence

import ijiri.ijiriserver.community.post.application.port.out.PostPort
import ijiri.ijiriserver.community.post.entity.Post
import ijiri.ijiriserver.community.post.entity.PostStatus
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PostPersistenceAdapter(
    private val postJpaRepository: PostJpaRepository,
) : PostPort {

    override fun findAllByMemberId(memberId: Long): List<Post> = postJpaRepository.findAllByMemberId(memberId)

    override fun countByMemberIdAndStatus(memberId: Long, status: PostStatus): Long =
        postJpaRepository.countByMemberIdAndStatus(memberId, status)

    override fun countByOwnedCarIds(ownedCarIds: Collection<Long>): List<Array<Any>> =
        postJpaRepository.countByOwnedCarIds(ownedCarIds)

    override fun hideAllByAuthor(memberId: Long) {
        postJpaRepository.hideAllByAuthor(memberId)
    }

    override fun delete(entity: Post) {
        postJpaRepository.delete(entity)
    }

    override fun findById(id: Long): Post? = postJpaRepository.findByIdOrNull(id)

    override fun save(entity: Post): Post = postJpaRepository.save(entity)

    override fun flush() {
        postJpaRepository.flush()
    }

    override fun findNewest(spec: Specification<Post>, limit: Int): List<Post> =
        postJpaRepository.findBy(spec) { query ->
            query
                .sortBy(Sort.by(Sort.Direction.DESC, "id"))
                .limit(limit)
                .all()
        }
}

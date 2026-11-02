package ijiri.ijiriserver.domain.post.adapter.out.persistence

import ijiri.ijiriserver.domain.post.application.port.out.PostPartPort
import ijiri.ijiriserver.domain.post.entity.PostPart
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PostPartPersistenceAdapter(
    private val postPartJpaRepository: PostPartJpaRepository,
) : PostPartPort {

    override fun findAllById(ids: Iterable<Long>): List<PostPart> = postPartJpaRepository.findAllById(ids)

    override fun findById(id: Long): PostPart? = postPartJpaRepository.findByIdOrNull(id)
}

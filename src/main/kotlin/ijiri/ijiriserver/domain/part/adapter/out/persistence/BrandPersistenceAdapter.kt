package ijiri.ijiriserver.domain.part.adapter.out.persistence

import ijiri.ijiriserver.domain.part.application.port.out.BrandPort
import ijiri.ijiriserver.domain.part.entity.Brand
import org.springframework.data.domain.Limit
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BrandPersistenceAdapter(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandPort {

    override fun findByNormalizedName(normalizedName: String): Brand? {
        return brandJpaRepository.findByNormalizedName(normalizedName)
    }

    override fun findByNormalizedNameContainingOrderByNameAsc(normalizedKeyword: String, limit: Limit): List<Brand> {
        return brandJpaRepository.findByNormalizedNameContainingOrderByNameAsc(normalizedKeyword, limit)
    }

    override fun insertIfAbsent(name: String, normalizedName: String, now: LocalDateTime) {
        brandJpaRepository.insertIfAbsent(name, normalizedName, now)
    }

    override fun findAllById(ids: Iterable<Long>): List<Brand> {
        return brandJpaRepository.findAllById(ids)
    }

    override fun findById(id: Long): Brand? {
        return brandJpaRepository.findByIdOrNull(id)
    }
}

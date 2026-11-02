package ijiri.ijiriserver.domain.part.application.port.out

import ijiri.ijiriserver.domain.part.entity.Brand
import org.springframework.data.domain.Limit
import java.time.LocalDateTime

interface BrandPort {

    fun findByNormalizedName(normalizedName: String): Brand?

    fun findByNormalizedNameContainingOrderByNameAsc(normalizedKeyword: String, limit: Limit): List<Brand>

    fun insertIfAbsent(name: String, normalizedName: String, now: LocalDateTime)

    fun findAllById(ids: Iterable<Long>): List<Brand>

    fun findById(id: Long): Brand?
}

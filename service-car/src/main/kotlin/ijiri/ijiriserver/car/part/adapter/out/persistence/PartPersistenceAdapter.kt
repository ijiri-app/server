package ijiri.ijiriserver.car.part.adapter.out.persistence

import ijiri.ijiriserver.car.part.application.data.PartSuggestion
import ijiri.ijiriserver.car.part.application.port.out.PartPort
import ijiri.ijiriserver.car.part.entity.Part
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class PartPersistenceAdapter(
    private val partJpaRepository: PartJpaRepository,
) : PartPort {

    override fun findByBrandIdAndNormalizedName(brandId: Long, normalizedName: String): Part? {
        return partJpaRepository.findByBrandIdAndNormalizedName(brandId, normalizedName)
    }

    override fun findByBrandIdIsNullAndNormalizedName(normalizedName: String): Part? {
        return partJpaRepository.findByBrandIdIsNullAndNormalizedName(normalizedName)
    }

    override fun insertIfAbsent(
        brandId: Long,
        name: String,
        normalizedName: String,
        category: String,
        now: LocalDateTime,
    ) {
        partJpaRepository.insertIfAbsent(brandId, name, normalizedName, category, now)
    }

    override fun insertWithoutBrandIfAbsent(
        name: String,
        normalizedName: String,
        category: String,
        now: LocalDateTime,
    ) {
        partJpaRepository.insertWithoutBrandIfAbsent(name, normalizedName, category, now)
    }

    override fun search(pattern: String, category: String, carModelId: Long, limit: Int): List<Part> {
        return partJpaRepository.search(pattern, category, carModelId, limit)
    }

    override fun suggest(
        keyword: String,
        pattern: String,
        category: String,
        carModelId: Long,
        limit: Int,
    ): List<PartSuggestion> {
        return partJpaRepository.suggest(keyword, pattern, category, carModelId, limit)
    }

    override fun addUseCount(ids: Collection<Long>, delta: Int) {
        partJpaRepository.addUseCount(ids, delta)
    }

    override fun findAllById(ids: Iterable<Long>): List<Part> {
        return partJpaRepository.findAllById(ids)
    }

    override fun findById(id: Long): Part? {
        return partJpaRepository.findByIdOrNull(id)
    }
}

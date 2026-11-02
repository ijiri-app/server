package ijiri.ijiriserver.domain.part.application.port.out

import ijiri.ijiriserver.domain.part.application.data.PartSuggestion
import ijiri.ijiriserver.domain.part.entity.Part
import java.time.LocalDateTime

interface PartPort {

    fun findByBrandIdAndNormalizedName(brandId: Long, normalizedName: String): Part?

    fun findByBrandIdIsNullAndNormalizedName(normalizedName: String): Part?

    fun insertIfAbsent(brandId: Long, name: String, normalizedName: String, category: String, now: LocalDateTime)

    fun insertWithoutBrandIfAbsent(name: String, normalizedName: String, category: String, now: LocalDateTime)

    fun search(pattern: String, category: String, carModelId: Long, limit: Int): List<Part>

    fun suggest(keyword: String, pattern: String, category: String, carModelId: Long, limit: Int): List<PartSuggestion>

    fun addUseCount(ids: Collection<Long>, delta: Int)

    fun findAllById(ids: Iterable<Long>): List<Part>

    fun findById(id: Long): Part?
}

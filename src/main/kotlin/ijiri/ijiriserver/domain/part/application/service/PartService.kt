package ijiri.ijiriserver.domain.part.application.service

import ijiri.ijiriserver.domain.part.application.data.PartCommand
import ijiri.ijiriserver.domain.part.application.data.PartInfo
import ijiri.ijiriserver.domain.part.application.data.response.PartResponse
import ijiri.ijiriserver.domain.part.application.port.`in`.GetBrandsUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.GetPartInfosUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.RecordPartUsageUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.ResolvePartUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.SearchPartsUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.SuggestPartsUseCase
import ijiri.ijiriserver.domain.part.application.port.out.BrandPort
import ijiri.ijiriserver.domain.part.application.port.out.PartCarModelUsagePort
import ijiri.ijiriserver.domain.part.application.port.out.PartPort
import ijiri.ijiriserver.domain.part.entity.Brand
import ijiri.ijiriserver.domain.part.entity.Part
import ijiri.ijiriserver.domain.part.entity.PartCategory
import ijiri.ijiriserver.domain.part.exception.PartStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale

@Service
@Transactional(readOnly = true)
class PartService(
    private val partPort: PartPort,
    private val brandPort: BrandPort,
    private val partCarModelUsagePort: PartCarModelUsagePort,
    private val clock: Clock,
) : SearchPartsUseCase,
    SuggestPartsUseCase,
    GetBrandsUseCase,
    ResolvePartUseCase,
    GetPartInfosUseCase,
    RecordPartUsageUseCase {

    override fun search(keyword: String?, category: PartCategory?, carModelId: Long?, size: Int): PartResponse {
        val parts = partPort.search(
            containsPattern(keyword),
            category?.name ?: "",
            carModelId ?: 0L,
            size,
        )
        return PartResponse.parts(parts, toInfos(parts))
    }

    // 키워드·별칭 부분 일치와 trigram 유사도를 합쳐 후보 3개. 임베딩(벡터) 검색은 아직 붙이지 않았다
    override fun suggest(keyword: String, category: PartCategory?, carModelId: Long?): PartResponse {
        val found = partPort.suggest(
            normalize(keyword),
            containsPattern(keyword),
            category?.name ?: "",
            carModelId ?: 0L,
            SUGGEST_LIMIT,
        ).filter { suggestion -> suggestion.score >= MIN_SUGGEST_SCORE }
        val infos = getPartInfos(found.map { it.id })
        val scores = found.associate { suggestion -> suggestion.id to round(suggestion.score) }
        return PartResponse.suggestions(
            found.map { suggestion -> infos.getValue(suggestion.id) },
            scores,
        )
    }

    override fun getBrands(keyword: String?): PartResponse {
        return PartResponse.brands(
            brandPort.findByNormalizedNameContainingOrderByNameAsc(
                normalize(keyword),
                Limit.of(BRAND_LIMIT),
            ),
        )
    }

    @Transactional
    override fun resolve(command: PartCommand): PartInfo {
        if (command.partId != null) {
            val part = partPort.findById(command.partId)
                ?: throw CustomException(PartStatusCode.PART_NOT_FOUND)
            return PartInfo.of(part, findBrand(part.brandId))
        }
        if (!StringUtils.hasText(command.partName) || command.category == null) {
            throw CustomException(PartStatusCode.INVALID_PART)
        }
        val brand = if (StringUtils.hasText(command.brandName)) findOrCreateBrand(command.brandName!!) else null
        return PartInfo.of(findOrCreatePart(brand, command.partName!!, command.category), brand)
    }

    override fun getPartInfos(partIds: Collection<Long>): Map<Long, PartInfo> {
        return toInfos(partPort.findAllById(partIds))
    }

    @Transactional
    override fun recordUsage(partIds: Collection<Long>, carModelId: Long, delta: Int) {
        if (partIds.isEmpty()) {
            return
        }
        partPort.addUseCount(partIds, delta)
        partIds.forEach { partId -> partCarModelUsagePort.add(partId, carModelId, delta) }
    }

    // 정규화한 이름이 완전히 같을 때만 같은 부품으로 보고, 분류는 처음 등록된 값을 따른다
    private fun findOrCreatePart(brand: Brand?, partName: String, category: PartCategory): Part {
        val name = cleanName(partName)
        val normalizedName = normalize(name)
        val now = LocalDateTime.now(clock)
        if (brand != null) {
            partPort.insertIfAbsent(brand.id, name, normalizedName, category.name, now)
            return partPort.findByBrandIdAndNormalizedName(brand.id, normalizedName)
                ?: throw NoSuchElementException()
        }
        partPort.insertWithoutBrandIfAbsent(name, normalizedName, category.name, now)
        return partPort.findByBrandIdIsNullAndNormalizedName(normalizedName) ?: throw NoSuchElementException()
    }

    private fun findOrCreateBrand(brandName: String): Brand {
        val name = cleanName(brandName)
        val normalizedName = normalize(name)
        brandPort.insertIfAbsent(name, normalizedName, LocalDateTime.now(clock))
        return brandPort.findByNormalizedName(normalizedName) ?: throw NoSuchElementException()
    }

    private fun findBrand(brandId: Long?): Brand? {
        return brandId?.let { brandPort.findById(it) }
    }

    private fun toInfos(parts: List<Part>): Map<Long, PartInfo> {
        val brandIds = parts.mapNotNull(Part::brandId).distinct()
        val brands = brandPort.findAllById(brandIds).associateBy(Brand::id)
        return parts.map { part -> PartInfo.of(part, part.brandId?.let { brands[it] }) }
            .associateBy(PartInfo::id)
    }

    // 화면에 보일 이름: 앞뒤 공백 제거, 연속 공백은 하나로
    private fun cleanName(name: String): String {
        return name.trim().replace(WHITESPACES, " ")
    }

    // 같은 부품 판단과 검색 기준: 대소문자, 띄어쓰기, 하이픈 차이를 무시한다 ("te-37" = "TE37")
    private fun normalize(name: String?): String {
        return name?.lowercase(Locale.ROOT)?.replace(SEPARATORS, "") ?: ""
    }

    private fun containsPattern(keyword: String?): String {
        val escaped = normalize(keyword).replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }

    private fun round(score: Double): Double {
        return Math.round(score * 100) / 100.0
    }

    companion object {
        private const val SUGGEST_LIMIT = 3

        // 이보다 낮은 유사도는 "혹시 이 부품인가요?" 후보로 보여주지 않는다
        private const val MIN_SUGGEST_SCORE = 0.3
        private const val BRAND_LIMIT = 30
        private val WHITESPACES = Regex("\\s+")
        private val SEPARATORS = Regex("[\\s\\-]+")
    }
}

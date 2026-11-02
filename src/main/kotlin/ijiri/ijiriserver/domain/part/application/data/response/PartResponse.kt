package ijiri.ijiriserver.domain.part.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.domain.part.application.data.PartInfo
import ijiri.ijiriserver.domain.part.entity.Brand
import ijiri.ijiriserver.domain.part.entity.Part
import ijiri.ijiriserver.domain.part.entity.PartCategory

/**
 * part 도메인의 모든 API 응답. 항목은 API 마다 필요한 필드만 채운다
 * (브랜드: id, name / 부품 검색: + brandName, category, useCount / 후보 추천: + score).
 */
data class PartResponse(
    val items: List<Item>,
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Item(
        val id: Long,
        val name: String,
        val brandName: String?,
        val category: PartCategory?,
        val useCount: Int?,
        val score: Double?,
    )

    companion object {
        fun parts(parts: List<Part>, infos: Map<Long, PartInfo>): PartResponse {
            return PartResponse(
                parts.map { part ->
                    Item(
                        part.id,
                        part.name,
                        infos.getValue(part.id).brandName,
                        part.category,
                        part.useCount,
                        null,
                    )
                },
            )
        }

        fun suggestions(parts: List<PartInfo>, scores: Map<Long, Double>): PartResponse {
            return PartResponse(
                parts.map { part -> Item(part.id, part.name, part.brandName, null, null, scores[part.id]) },
            )
        }

        fun brands(brands: List<Brand>): PartResponse {
            return PartResponse(
                brands.map { brand -> Item(brand.id, brand.name, null, null, null, null) },
            )
        }
    }
}

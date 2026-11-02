package ijiri.ijiriserver.domain.part.application.data

import ijiri.ijiriserver.domain.part.entity.Brand
import ijiri.ijiriserver.domain.part.entity.Part
import ijiri.ijiriserver.domain.part.entity.PartCategory

data class PartInfo(
    val id: Long,
    val name: String,
    val category: PartCategory,
    val brandId: Long?,
    val brandName: String?,
) {

    companion object {
        fun of(part: Part, brand: Brand?): PartInfo {
            return PartInfo(
                part.id,
                part.name,
                part.category,
                part.brandId,
                brand?.name,
            )
        }
    }
}

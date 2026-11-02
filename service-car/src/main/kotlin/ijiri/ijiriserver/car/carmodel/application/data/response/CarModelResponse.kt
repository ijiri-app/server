package ijiri.ijiriserver.car.carmodel.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.car.carmodel.entity.CarGeneration
import ijiri.ijiriserver.car.carmodel.entity.CarModel
import ijiri.ijiriserver.car.carmodel.entity.CarTrim

/**
 * carmodel 도메인의 모든 API 응답. 목록은 items, 상세는 모델 필드와 generations 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CarModelResponse(
    val id: Long?,
    val brand: String?,
    val name: String?,
    val generations: List<Generation>?,
    val items: List<Item>?,
) {

    data class Item(
        val id: Long,
        val brand: String,
        val name: String,
        val isCore: Boolean,
        val imageUrl: String?,
    ) {

        companion object {
            fun from(model: CarModel): Item {
                return Item(model.id, model.brand, model.name, model.core, model.imageUrl)
            }
        }
    }

    data class Generation(
        val id: Long,
        val code: String,
        val startYear: Int,
        val endYear: Int?,
        val trims: List<Trim>,
    )

    data class Trim(
        val id: Long,
        val name: String,
    )

    companion object {
        fun list(models: List<CarModel>): CarModelResponse {
            return CarModelResponse(
                null,
                null,
                null,
                null,
                models.map(Item::from),
            )
        }

        fun detail(model: CarModel, generations: List<CarGeneration>, trims: List<CarTrim>): CarModelResponse {
            val trimsByGeneration = trims.groupBy(
                keySelector = CarTrim::carGenerationId,
                valueTransform = { trim -> Trim(trim.id, trim.name) },
            )
            return CarModelResponse(
                model.id,
                model.brand,
                model.name,
                generations.map { generation ->
                    Generation(
                        generation.id,
                        generation.code,
                        generation.startYear,
                        generation.endYear,
                        trimsByGeneration[generation.id] ?: listOf(),
                    )
                },
                null,
            )
        }
    }
}

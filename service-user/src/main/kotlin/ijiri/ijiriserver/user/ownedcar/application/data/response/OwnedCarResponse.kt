package ijiri.ijiriserver.user.ownedcar.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.common.contract.BuildStyle
import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus

/**
 * ownedcar 도메인의 모든 API 응답. 목록은 items, 등록은 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OwnedCarResponse(
    val id: Long?,
    val items: List<Item>?,
) {

    data class Item(
        val id: Long,
        val carModelId: Long,
        val carModelName: String,
        val generationCode: String,
        val trimId: Long,
        val trimName: String,
        val year: Int,
        val buildStyle: BuildStyle?,
        val nickname: String?,
        val status: OwnedCarStatus,
        val postCount: Long,
    ) {

        companion object {
            fun of(car: OwnedCar, spec: CarSpec, postCount: Long): Item {
                return Item(
                    car.id,
                    spec.carModelId,
                    spec.modelName,
                    spec.generationCode,
                    spec.trimId,
                    spec.trimName,
                    car.modelYear,
                    car.buildStyle,
                    car.nickname,
                    car.status,
                    postCount,
                )
            }
        }
    }

    companion object {
        fun created(id: Long): OwnedCarResponse {
            return OwnedCarResponse(id, null)
        }

        fun list(items: List<Item>): OwnedCarResponse {
            return OwnedCarResponse(null, items)
        }
    }
}

package ijiri.ijiriserver.domain.carmodel.application.data

import ijiri.ijiriserver.domain.carmodel.entity.CarModel

data class CarModelInfo(
    val id: Long,
    val brand: String,
    val name: String,
) {

    companion object {
        fun from(model: CarModel): CarModelInfo {
            return CarModelInfo(model.id, model.brand, model.name)
        }
    }
}

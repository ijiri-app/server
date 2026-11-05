package ijiri.ijiriserver.user.interestcar.application.data.response

import ijiri.ijiriserver.common.contract.CarModelInfo

data class InterestCarResponse(
    val items: List<Item>,
) {

    data class Item(
        val id: Long,
        val brand: String,
        val name: String,
    )

    companion object {
        fun from(carModels: List<CarModelInfo>): InterestCarResponse {
            return InterestCarResponse(
                carModels.map { model -> Item(model.id, model.brand, model.name) },
            )
        }
    }
}

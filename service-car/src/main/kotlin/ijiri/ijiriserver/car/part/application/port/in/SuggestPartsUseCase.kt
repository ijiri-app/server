package ijiri.ijiriserver.car.part.application.port.`in`

import ijiri.ijiriserver.car.part.application.data.response.PartResponse
import ijiri.ijiriserver.common.contract.PartCategory

interface SuggestPartsUseCase {

    fun suggest(keyword: String, category: PartCategory?, carModelId: Long?): PartResponse
}

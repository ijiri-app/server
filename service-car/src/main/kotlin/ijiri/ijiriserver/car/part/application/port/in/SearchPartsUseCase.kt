package ijiri.ijiriserver.car.part.application.port.`in`

import ijiri.ijiriserver.car.part.application.data.response.PartResponse
import ijiri.ijiriserver.common.contract.PartCategory

interface SearchPartsUseCase {

    fun search(keyword: String?, category: PartCategory?, carModelId: Long?, size: Int): PartResponse
}

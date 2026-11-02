package ijiri.ijiriserver.domain.part.application.port.`in`

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse
import ijiri.ijiriserver.domain.part.entity.PartCategory

interface SearchPartsUseCase {

    fun search(keyword: String?, category: PartCategory?, carModelId: Long?, size: Int): PartResponse
}

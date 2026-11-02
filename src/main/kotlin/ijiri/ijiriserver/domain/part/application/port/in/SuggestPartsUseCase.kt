package ijiri.ijiriserver.domain.part.application.port.`in`

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse
import ijiri.ijiriserver.domain.part.entity.PartCategory

interface SuggestPartsUseCase {

    fun suggest(keyword: String, category: PartCategory?, carModelId: Long?): PartResponse
}

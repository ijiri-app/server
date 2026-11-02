package ijiri.ijiriserver.domain.part.application.port.`in`

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse

interface GetBrandsUseCase {

    fun getBrands(keyword: String?): PartResponse
}

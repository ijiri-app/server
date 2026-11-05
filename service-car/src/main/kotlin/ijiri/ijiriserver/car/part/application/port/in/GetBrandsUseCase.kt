package ijiri.ijiriserver.car.part.application.port.`in`

import ijiri.ijiriserver.car.part.application.data.response.PartResponse

interface GetBrandsUseCase {

    fun getBrands(keyword: String?): PartResponse
}

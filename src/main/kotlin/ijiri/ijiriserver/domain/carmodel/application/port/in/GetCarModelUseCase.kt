package ijiri.ijiriserver.domain.carmodel.application.port.`in`

import ijiri.ijiriserver.domain.carmodel.application.data.response.CarModelResponse

interface GetCarModelUseCase {

    fun getCarModel(carModelId: Long): CarModelResponse
}

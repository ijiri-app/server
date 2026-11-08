package ijiri.ijiriserver.car.carmodel.application.port.`in`

import ijiri.ijiriserver.car.carmodel.application.data.response.CarModelResponse

interface GetCarModelUseCase {

    fun getCarModel(carModelId: Long): CarModelResponse
}

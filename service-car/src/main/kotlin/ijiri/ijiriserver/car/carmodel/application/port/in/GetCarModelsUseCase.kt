package ijiri.ijiriserver.car.carmodel.application.port.`in`

import ijiri.ijiriserver.car.carmodel.application.data.response.CarModelResponse

interface GetCarModelsUseCase {

    fun getCarModels(keyword: String?, coreOnly: Boolean): CarModelResponse
}

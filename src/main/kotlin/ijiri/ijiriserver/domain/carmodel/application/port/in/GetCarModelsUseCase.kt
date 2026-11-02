package ijiri.ijiriserver.domain.carmodel.application.port.`in`

import ijiri.ijiriserver.domain.carmodel.application.data.response.CarModelResponse

interface GetCarModelsUseCase {

    fun getCarModels(keyword: String?, coreOnly: Boolean): CarModelResponse
}

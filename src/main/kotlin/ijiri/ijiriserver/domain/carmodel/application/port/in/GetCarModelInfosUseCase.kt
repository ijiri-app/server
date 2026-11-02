package ijiri.ijiriserver.domain.carmodel.application.port.`in`

import ijiri.ijiriserver.domain.carmodel.application.data.CarModelInfo

interface GetCarModelInfosUseCase {

    /**
     * 요청 순서대로 돌려준다. 없는 id 는 빠진다.
     */
    fun getCarModelInfos(carModelIds: List<Long>): List<CarModelInfo>
}

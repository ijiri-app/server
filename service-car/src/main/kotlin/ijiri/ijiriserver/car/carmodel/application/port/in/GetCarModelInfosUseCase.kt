package ijiri.ijiriserver.car.carmodel.application.port.`in`

import ijiri.ijiriserver.common.contract.CarModelInfo

interface GetCarModelInfosUseCase {

    /**
     * 숨긴 차종도 hidden 표시와 함께 돌려준다. 없는 id 는 빠진다.
     */
    fun getCarModelInfos(carModelIds: Collection<Long>): List<CarModelInfo>
}

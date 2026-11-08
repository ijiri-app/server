package ijiri.ijiriserver.car.carmodel.application.port.`in`

import ijiri.ijiriserver.common.contract.CarSpec

interface GetCarSpecByTrimUseCase {

    /**
     * 트림으로 모델·세대를 정한다. 없는 트림이면 INVALID_TRIM.
     */
    fun getSpecByTrim(trimId: Long): CarSpec

    /**
     * 여러 트림을 한 번에. 없는 id 는 빠진다.
     */
    fun getSpecsByTrims(trimIds: Collection<Long>): List<CarSpec>
}

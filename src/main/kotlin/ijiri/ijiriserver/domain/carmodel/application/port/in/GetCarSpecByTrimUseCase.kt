package ijiri.ijiriserver.domain.carmodel.application.port.`in`

import ijiri.ijiriserver.domain.carmodel.entity.CarSpec

interface GetCarSpecByTrimUseCase {

    /**
     * 트림으로 모델·세대를 정한다. 없는 트림이면 INVALID_TRIM.
     */
    fun getSpecByTrim(trimId: Long): CarSpec
}

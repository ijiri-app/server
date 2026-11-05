package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.common.contract.CarSpec

/**
 * 차종 마스터는 service-car 에 있다.
 */
interface CarReader {

    /**
     * 없는 트림이면 INVALID_TRIM.
     */
    fun getSpecByTrim(trimId: Long): CarSpec

    /**
     * 차종 id 별 이름. 없는 id 는 빠진다.
     */
    fun getModelNames(carModelIds: Collection<Long>): Map<Long, String>
}

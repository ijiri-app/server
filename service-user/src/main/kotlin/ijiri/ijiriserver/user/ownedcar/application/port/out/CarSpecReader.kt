package ijiri.ijiriserver.user.ownedcar.application.port.out

import ijiri.ijiriserver.common.contract.CarSpec

/**
 * 차종 마스터는 service-car 에 있으므로 내부 API 로 읽는다.
 */
interface CarSpecReader {

    /**
     * 없는 트림이면 INVALID_TRIM.
     */
    fun getSpecByTrim(trimId: Long): CarSpec

    /**
     * 트림 id 별. 없는 id 는 빠진다.
     */
    fun getSpecsByTrims(trimIds: Collection<Long>): Map<Long, CarSpec>
}

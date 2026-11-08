package ijiri.ijiriserver.user.interestcar.application.port.out

import ijiri.ijiriserver.common.contract.CarModelInfo

/**
 * 차종 마스터는 service-car 에 있으므로 내부 API 로 읽는다. 없는 id 는 빠지고 숨긴 차종은 hidden 으로 온다.
 */
interface CarModelReader {

    fun getCarModelInfos(carModelIds: Collection<Long>): List<CarModelInfo>
}

package ijiri.ijiriserver.domain.part.application.port.`in`

import ijiri.ijiriserver.domain.part.application.data.PartInfo

interface GetPartInfosUseCase {

    /**
     * 없는 id 는 결과에서 빠진다.
     */
    fun getPartInfos(partIds: Collection<Long>): Map<Long, PartInfo>
}

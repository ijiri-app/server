package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.common.contract.PartInfo
import ijiri.ijiriserver.common.contract.PartResolveRequest

/**
 * 부품과 브랜드는 service-car 에 있다.
 */
interface PartReader {

    /**
     * 기존 부품을 확인하거나 새 부품을 확인 대기로 등록한다.
     */
    fun resolve(request: PartResolveRequest): PartInfo

    /**
     * 없는 id 는 빠진다.
     */
    fun getPartInfos(partIds: Collection<Long>): Map<Long, PartInfo>
}

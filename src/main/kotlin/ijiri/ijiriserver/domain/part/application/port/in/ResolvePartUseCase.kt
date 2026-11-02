package ijiri.ijiriserver.domain.part.application.port.`in`

import ijiri.ijiriserver.domain.part.application.data.PartCommand
import ijiri.ijiriserver.domain.part.application.data.PartInfo

interface ResolvePartUseCase {

    /**
     * 기존 부품을 확인하거나, 정규화한 이름이 같은 부품이 없으면 확인 대기 부품으로 새로 만든다 (브랜드도 같은 방식).
     */
    fun resolve(command: PartCommand): PartInfo
}

package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot

interface GetOwnedCarSnapshotsUseCase {

    /**
     * 게시물 상세에 보여줄 차량 정보. 없는 id 는 빠진다.
     */
    fun getSnapshots(ownedCarIds: Collection<Long>): Map<Long, OwnedCarSnapshot>
}

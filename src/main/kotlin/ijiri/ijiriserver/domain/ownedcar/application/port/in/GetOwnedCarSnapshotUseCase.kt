package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot

interface GetOwnedCarSnapshotUseCase {

    /**
     * 남의 차량이면 FORBIDDEN, 없으면 NOT_FOUND.
     */
    fun getSnapshot(memberId: Long, ownedCarId: Long): OwnedCarSnapshot
}

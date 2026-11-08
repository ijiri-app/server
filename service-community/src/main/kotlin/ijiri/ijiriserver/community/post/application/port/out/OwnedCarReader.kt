package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.common.contract.OwnedCarSnapshot

/**
 * 보유 차량은 service-user 에 있다.
 */
interface OwnedCarReader {

    /**
     * 남의 차량이면 FORBIDDEN, 없으면 NOT_FOUND.
     */
    fun getSnapshot(memberId: Long, ownedCarId: Long): OwnedCarSnapshot
}

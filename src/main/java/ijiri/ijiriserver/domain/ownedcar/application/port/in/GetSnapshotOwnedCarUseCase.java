package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot;

public interface GetSnapshotOwnedCarUseCase {

    /**
     * 남의 차량이면 FORBIDDEN, 없으면 NOT_FOUND.
     */
    OwnedCarSnapshot getSnapshot(Long memberId, Long ownedCarId);
}

package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot;

import java.util.Collection;
import java.util.Map;

public interface GetSnapshotsOwnedCarUseCase {

    /**
     * 게시물 상세에 보여줄 차량 정보. 없는 id 는 빠진다.
     */
    Map<Long, OwnedCarSnapshot> getSnapshots(Collection<Long> ownedCarIds);
}

package ijiri.ijiriserver.domain.ownedcar.service;

import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarUpdateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.response.OwnedCarResponse;

import java.util.Collection;
import java.util.Map;

public interface OwnedCarService {

    OwnedCarResponse getAll(Long memberId);

    OwnedCarResponse create(Long memberId, OwnedCarCreateRequest request);

    void update(Long memberId, Long ownedCarId, OwnedCarUpdateRequest request);

    /**
     * 게시물이 연결된 차량은 지우지 않고 PAST 로 바꾼다.
     */
    void delete(Long memberId, Long ownedCarId);

    /**
     * 남의 차량이면 FORBIDDEN, 없으면 NOT_FOUND.
     */
    OwnedCarSnapshot getSnapshot(Long memberId, Long ownedCarId);

    /**
     * 게시물 상세에 보여줄 차량 정보. 없는 id 는 빠진다.
     */
    Map<Long, OwnedCarSnapshot> getSnapshots(Collection<Long> ownedCarIds);
}

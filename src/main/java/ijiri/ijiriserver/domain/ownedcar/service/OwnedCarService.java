package ijiri.ijiriserver.domain.ownedcar.service;

import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarUpdateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.response.OwnedCarResponse;

public interface OwnedCarService {

    OwnedCarResponse getAll(Long memberId);

    OwnedCarResponse create(Long memberId, OwnedCarCreateRequest request);

    OwnedCarResponse update(Long memberId, Long ownedCarId, OwnedCarUpdateRequest request);

    OwnedCarResponse delete(Long memberId, Long ownedCarId);

    /**
     * 이 회원의 보유 차량이 아니면 OWNEDCAR404.
     */
    OwnedCarSnapshot getSnapshot(Long memberId, Long ownedCarId);
}

package ijiri.ijiriserver.domain.ownedcar.dto;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;

/**
 * 게시물에 복사해 두는 보유 차량 정보. 나중에 보유 차량을 고치거나 지워도 게시물의 차량 정보는 그대로 남는다.
 */
public record OwnedCarSnapshot(
        CarSpec spec,
        Integer modelYear,
        BuildDirection buildDirection
) {
}

package ijiri.ijiriserver.domain.ownedcar.dto;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;

/**
 * 게시물이 참조하는 보유 차량 정보. 게시물에는 차종·트림·연식을 함께 복사해 피드 필터와 카드에 쓴다.
 */
public record OwnedCarSnapshot(
        Long ownedCarId,
        CarSpec spec,
        int modelYear
) {
}

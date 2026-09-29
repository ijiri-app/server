package ijiri.ijiriserver.domain.interestcar.dto.response;

import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;

import java.util.List;

public record InterestCarResponse(
        List<Long> carModelIds
) {

    public static InterestCarResponse from(List<MemberInterestCar> interestCars) {
        return new InterestCarResponse(
                interestCars.stream()
                .map(MemberInterestCar::getCarModelId)
                .toList()
        );
    }
}

package ijiri.ijiriserver.domain.interestcar.dto.response;

import ijiri.ijiriserver.domain.carmodel.dto.CarModelInfo;

import java.util.List;

public record InterestCarResponse(
        List<Item> items
) {

    public static InterestCarResponse from(List<CarModelInfo> carModels) {
        return new InterestCarResponse(
                carModels.stream()
                        .map(model -> new Item(model.id(), model.brand(), model.name()))
                        .toList()
        );
    }

    public record Item(
            Long id,
            String brand,
            String name
    ) {
    }
}

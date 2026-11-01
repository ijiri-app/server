package ijiri.ijiriserver.domain.interestcar.application.data.response;

import ijiri.ijiriserver.domain.carmodel.application.data.CarModelInfo;

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

package ijiri.ijiriserver.domain.carmodel.dto;

import ijiri.ijiriserver.domain.carmodel.entity.CarModel;

public record CarModelInfo(
        Long id,
        String brand,
        String name
) {

    public static CarModelInfo from(CarModel model) {
        return new CarModelInfo(model.getId(), model.getBrand(), model.getName());
    }
}

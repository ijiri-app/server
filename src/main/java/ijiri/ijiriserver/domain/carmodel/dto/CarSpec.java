package ijiri.ijiriserver.domain.carmodel.dto;

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;

/**
 * 트림 하나로 정해지는 모델 / 세대 / 트림 조합.
 */
public record CarSpec(
        Long carModelId,
        String brand,
        String modelName,
        Long generationId,
        String generationCode,
        Long trimId,
        String trimName
) {

    public static CarSpec of(CarModel model, CarGeneration generation, CarTrim trim) {
        return new CarSpec(
                model.getId(),
                model.getBrand(),
                model.getName(),
                generation.getId(),
                generation.getCode(),
                trim.getId(),
                trim.getName()
        );
    }
}

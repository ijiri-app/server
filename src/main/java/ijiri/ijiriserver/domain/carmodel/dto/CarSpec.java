package ijiri.ijiriserver.domain.carmodel.dto;

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;

/**
 * 검증된 모델 / 세대 / 트림 조합. 트림은 선택이라 trimId, trimName 이 null 일 수 있다.
 */
public record CarSpec(
        Long carModelId,
        String manufacturer,
        String modelName,
        Long generationId,
        String generationCode,
        String generationName,
        Long trimId,
        String trimName
) {

    public static CarSpec of(CarModel model, CarGeneration generation, CarTrim trim) {
        return new CarSpec(
                model.getId(),
                model.getManufacturer(),
                model.getName(),
                generation.getId(),
                generation.getCode(),
                generation.getName(),
                trim != null ? trim.getId() : null,
                trim != null ? trim.getName() : null
        );
    }
}

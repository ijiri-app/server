package ijiri.ijiriserver.domain.ownedcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;

import java.util.List;

/**
 * ownedcar 도메인의 모든 API 응답. 목록은 cars, 등록/수정은 car, 삭제는 message 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OwnedCarResponse(
        List<Car> cars,
        Car car,
        String message
) {

    public static OwnedCarResponse list(List<Car> cars) {
        return new OwnedCarResponse(cars, null, null);
    }

    public static OwnedCarResponse single(Car car) {
        return new OwnedCarResponse(null, car, null);
    }

    public static OwnedCarResponse message(String message) {
        return new OwnedCarResponse(null, null, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Car(
            Long id,
            Long carModelId,
            String manufacturer,
            String modelName,
            Long carGenerationId,
            String generationCode,
            String generationName,
            Long carTrimId,
            String trimName,
            Integer modelYear,
            BuildDirection buildDirection
    ) {

        public static Car of(OwnedCar car, CarSpec spec) {
            return new Car(
                    car.getId(),
                    spec.carModelId(),
                    spec.manufacturer(),
                    spec.modelName(),
                    spec.generationId(),
                    spec.generationCode(),
                    spec.generationName(),
                    spec.trimId(),
                    spec.trimName(),
                    car.getModelYear(),
                    car.getBuildDirection()
            );
        }
    }
}

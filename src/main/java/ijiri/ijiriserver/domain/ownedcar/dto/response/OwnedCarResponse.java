package ijiri.ijiriserver.domain.ownedcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus;

import java.util.List;

/**
 * ownedcar 도메인의 모든 API 응답. 목록은 items, 등록은 id 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OwnedCarResponse(
        Long id,
        List<Item> items
) {

    public static OwnedCarResponse created(Long id) {
        return new OwnedCarResponse(id, null);
    }

    public static OwnedCarResponse list(List<Item> items) {
        return new OwnedCarResponse(null, items);
    }

    public record Item(
            Long id,
            Long carModelId,
            String carModelName,
            String generationCode,
            Long trimId,
            String trimName,
            int year,
            BuildStyle buildStyle,
            String nickname,
            OwnedCarStatus status,
            long postCount
    ) {

        public static Item of(OwnedCar car, CarSpec spec, long postCount) {
            return new Item(
                    car.getId(),
                    spec.carModelId(),
                    spec.modelName(),
                    spec.generationCode(),
                    spec.trimId(),
                    spec.trimName(),
                    car.getModelYear(),
                    car.getBuildStyle(),
                    car.getNickname(),
                    car.getStatus(),
                    postCount
            );
        }
    }
}

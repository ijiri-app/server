package ijiri.ijiriserver.domain.carmodel.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * carmodel 도메인의 모든 API 응답. 목록은 carModels, 상세는 carModel 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarModelResponse(
        List<Model> carModels,
        Model carModel
) {

    public static CarModelResponse list(List<CarModel> models) {
        return new CarModelResponse(
                models.stream()
                        .map(model -> Model.of(model, null))
                        .toList(),
                null
        );
    }

    public static CarModelResponse detail(CarModel model, List<CarGeneration> generations, List<CarTrim> trims) {
        Map<Long, List<Trim>> trimsByGeneration = trims.stream()
                .collect(Collectors.groupingBy(
                        CarTrim::getCarGenerationId,
                        Collectors.mapping(trim -> new Trim(trim.getId(), trim.getName()), Collectors.toList())
                ));
        List<Generation> generationResponses = generations.stream()
                .map(generation -> new Generation(
                        generation.getId(),
                        generation.getCode(),
                        generation.getName(),
                        generation.getStartYear(),
                        generation.getEndYear(),
                        trimsByGeneration.getOrDefault(generation.getId(), List.of())
                ))
                .toList();
        return new CarModelResponse(null, Model.of(model, generationResponses));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Model(
            Long id,
            String manufacturer,
            String name,
            List<Generation> generations
    ) {

        static Model of(CarModel model, List<Generation> generations) {
            return new Model(model.getId(), model.getManufacturer(), model.getName(), generations);
        }
    }

    public record Generation(
            Long id,
            String code,
            String name,
            int startYear,
            Integer endYear,
            List<Trim> trims
    ) {
    }

    public record Trim(
            Long id,
            String name
    ) {
    }
}

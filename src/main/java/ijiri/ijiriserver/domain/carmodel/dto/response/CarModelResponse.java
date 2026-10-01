package ijiri.ijiriserver.domain.carmodel.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * carmodel 도메인의 모든 API 응답. 목록은 items, 상세는 모델 필드와 generations 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarModelResponse(
        Long id,
        String brand,
        String name,
        List<Generation> generations,
        List<Item> items
) {

    public static CarModelResponse list(List<CarModel> models) {
        return new CarModelResponse(
                null,
                null,
                null,
                null,
                models.stream()
                        .map(Item::from)
                        .toList()
        );
    }

    public static CarModelResponse detail(CarModel model, List<CarGeneration> generations, List<CarTrim> trims) {
        Map<Long, List<Trim>> trimsByGeneration = trims.stream()
                .collect(Collectors.groupingBy(
                        CarTrim::getCarGenerationId,
                        Collectors.mapping(trim -> new Trim(trim.getId(), trim.getName()), Collectors.toList())
                ));
        return new CarModelResponse(
                model.getId(),
                model.getBrand(),
                model.getName(),
                generations.stream()
                        .map(generation -> new Generation(
                                generation.getId(),
                                generation.getCode(),
                                generation.getStartYear(),
                                generation.getEndYear(),
                                trimsByGeneration.getOrDefault(generation.getId(), List.of())
                        ))
                        .toList(),
                null
        );
    }

    public record Item(
            Long id,
            String brand,
            String name,
            boolean isCore,
            String imageUrl
    ) {

        static Item from(CarModel model) {
            return new Item(model.getId(), model.getBrand(), model.getName(), model.isCore(), model.getImageUrl());
        }
    }

    public record Generation(
            Long id,
            String code,
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

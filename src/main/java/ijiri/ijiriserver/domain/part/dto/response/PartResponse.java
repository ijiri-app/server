package ijiri.ijiriserver.domain.part.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.entity.Brand;
import ijiri.ijiriserver.domain.part.entity.Part;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

import java.util.List;
import java.util.Map;

/**
 * part 도메인의 모든 API 응답. 항목은 API 마다 필요한 필드만 채운다
 * (브랜드: id, name / 부품 검색: + brandName, category, useCount / 후보 추천: + score).
 */
public record PartResponse(
        List<Item> items
) {

    public static PartResponse parts(List<Part> parts, Map<Long, PartInfo> infos) {
        return new PartResponse(parts.stream()
                .map(part -> new Item(
                        part.getId(),
                        part.getName(),
                        infos.get(part.getId()).brandName(),
                        part.getCategory(),
                        part.getUseCount(),
                        null
                ))
                .toList()
        );
    }

    public static PartResponse suggestions(List<PartInfo> parts, Map<Long, Double> scores) {
        return new PartResponse(parts.stream()
                .map(part -> new Item(part.id(), part.name(), part.brandName(), null, null, scores.get(part.id())))
                .toList()
        );
    }

    public static PartResponse brands(List<Brand> brands) {
        return new PartResponse(brands.stream()
                .map(brand -> new Item(brand.getId(), brand.getName(), null, null, null, null))
                .toList()
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            Long id,
            String name,
            String brandName,
            PartCategory category,
            Integer useCount,
            Double score
    ) {
    }
}

package ijiri.ijiriserver.domain.part.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.entity.Brand;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

import java.util.List;

/**
 * part 도메인의 모든 API 응답. 부품 검색은 parts(+nextCursor), 브랜드 검색은 brands 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartResponse(
        List<PartItem> parts,
        Long nextCursor,
        List<BrandItem> brands
) {

    public static PartResponse parts(List<PartInfo> parts, Long nextCursor) {
        return new PartResponse(
                parts.stream()
                        .map(PartItem::from)
                        .toList(),
                nextCursor,
                null
        );
    }

    public static PartResponse brands(List<Brand> brands) {
        return new PartResponse(
                null,
                null,
                brands.stream()
                        .map(brand -> new BrandItem(brand.getId(), brand.getName()))
                        .toList()
        );
    }

    public record PartItem(
            Long id,
            String name,
            PartCategory category,
            Long brandId,
            String brandName
    ) {

        static PartItem from(PartInfo part) {
            return new PartItem(part.id(), part.name(), part.category(), part.brandId(), part.brandName());
        }
    }

    public record BrandItem(
            Long id,
            String name
    ) {
    }
}

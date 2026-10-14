package ijiri.ijiriserver.domain.part.dto;

import ijiri.ijiriserver.domain.part.entity.Brand;
import ijiri.ijiriserver.domain.part.entity.Part;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

public record PartInfo(
        Long id,
        String name,
        PartCategory category,
        Long brandId,
        String brandName
) {

    public static PartInfo of(Part part, Brand brand) {
        return new PartInfo(
                part.getId(),
                part.getName(),
                part.getCategory(),
                part.getBrandId(),
                brand != null ? brand.getName() : null
        );
    }
}

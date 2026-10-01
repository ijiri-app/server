package ijiri.ijiriserver.domain.part.service.impl;

import ijiri.ijiriserver.domain.part.dto.PartCommand;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.dto.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.Brand;
import ijiri.ijiriserver.domain.part.entity.Part;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.part.exception.PartStatusCode;
import ijiri.ijiriserver.domain.part.repository.BrandRepository;
import ijiri.ijiriserver.domain.part.repository.PartRepository;
import ijiri.ijiriserver.domain.part.service.PartService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartServiceImpl implements PartService {

    private static final int SUGGEST_LIMIT = 20;
    private static final int BRAND_LIMIT = 30;

    private final PartRepository partRepository;
    private final BrandRepository brandRepository;
    private final Clock clock;

    @Override
    public PartResponse suggest(String keyword, PartCategory category) {
        String trimmed = keyword.strip();
        List<Part> parts = partRepository.suggest(
                trimmed,
                "%" + escapeLike(trimmed) + "%",
                category != null ? category.name() : "",
                SUGGEST_LIMIT
        );
        return PartResponse.parts(toInfos(parts), null);
    }

    @Override
    public PartResponse getParts(String keyword, PartCategory category, Long brandId, Long cursor, int size) {
        String pattern = StringUtils.hasText(keyword)
                ? "%" + escapeLike(keyword.strip().toLowerCase(Locale.ROOT)) + "%"
                : null;
        Specification<Part> spec = (root, query, cb) -> cb.and(
                pattern != null ? cb.like(cb.lower(root.get("name")), pattern, '\\') : cb.conjunction(),
                category != null ? cb.equal(root.get("category"), category) : cb.conjunction(),
                brandId != null ? cb.equal(root.get("brandId"), brandId) : cb.conjunction(),
                cursor != null ? cb.lessThan(root.get("id"), cursor) : cb.conjunction()
        );
        List<Part> found = partRepository.findBy(spec, q -> q
                .sortBy(Sort.by(Sort.Direction.DESC, "id"))
                .limit(size + 1)
                .all()
        );
        boolean hasNext = found.size() > size;
        List<Part> page = hasNext ? found.subList(0, size) : found;
        return PartResponse.parts(toInfos(page), hasNext ? page.getLast().getId() : null);
    }

    @Override
    public PartResponse getBrands(String keyword) {
        String trimmed = keyword == null ? "" : keyword.strip();
        return PartResponse.brands(brandRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                trimmed,
                Limit.of(BRAND_LIMIT)
        ));
    }

    @Override
    @Transactional
    public PartInfo resolve(PartCommand command) {
        if (command.partId() != null) {
            Part part = partRepository.findById(command.partId())
                    .orElseThrow(() -> new CustomException(PartStatusCode.PART_NOT_FOUND));
            return PartInfo.of(part, findBrand(part.getBrandId()));
        }
        if (!StringUtils.hasText(command.name()) || command.category() == null) {
            throw new CustomException(PartStatusCode.INVALID_PART);
        }
        Brand brand = StringUtils.hasText(command.brandName()) ? findOrCreateBrand(command.brandName()) : null;
        return PartInfo.of(findOrCreatePart(brand, command), brand);
    }

    @Override
    public Map<Long, PartInfo> getPartInfos(Collection<Long> partIds) {
        return toInfos(partRepository.findAllById(partIds)).stream()
                .collect(Collectors.toMap(PartInfo::id, Function.identity()));
    }

    // 이미 같은 이름(정규화 기준)의 부품이 있으면 그 부품을 쓰고, 분류는 처음 등록된 값을 따른다
    private Part findOrCreatePart(Brand brand, PartCommand command) {
        String name = cleanName(command.name());
        String normalizedName = normalize(name);
        Long brandId = brand != null ? brand.getId() : null;
        LocalDateTime now = LocalDateTime.now(clock);
        if (brandId != null) {
            partRepository.insertIfAbsent(brandId, name, normalizedName, command.category().name(), now);
            return partRepository.findByBrandIdAndNormalizedName(brandId, normalizedName).orElseThrow();
        }
        partRepository.insertWithoutBrandIfAbsent(name, normalizedName, command.category().name(), now);
        return partRepository.findByBrandIdIsNullAndNormalizedName(normalizedName).orElseThrow();
    }

    private Brand findOrCreateBrand(String brandName) {
        String name = cleanName(brandName);
        String normalizedName = normalize(name);
        brandRepository.insertIfAbsent(name, normalizedName, LocalDateTime.now(clock));
        return brandRepository.findByNormalizedName(normalizedName).orElseThrow();
    }

    private Brand findBrand(Long brandId) {
        return brandId == null ? null : brandRepository.findById(brandId).orElse(null);
    }

    private List<PartInfo> toInfos(List<Part> parts) {
        List<Long> brandIds = parts.stream()
                .map(Part::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Brand> brands = brandRepository.findAllById(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));
        return parts.stream()
                .map(part -> PartInfo.of(part, part.getBrandId() != null ? brands.get(part.getBrandId()) : null))
                .toList();
    }

    // 화면에 보일 이름: 앞뒤 공백 제거, 연속 공백은 하나로
    private String cleanName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    // 같은 부품 판단 기준: 대소문자, 공백 차이를 무시한다
    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

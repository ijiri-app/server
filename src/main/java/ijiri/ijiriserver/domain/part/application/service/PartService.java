package ijiri.ijiriserver.domain.part.application.service;

import ijiri.ijiriserver.domain.part.application.data.PartCommand;
import ijiri.ijiriserver.domain.part.application.data.PartInfo;
import ijiri.ijiriserver.domain.part.application.data.PartSuggestion;
import ijiri.ijiriserver.domain.part.application.data.response.PartResponse;
import ijiri.ijiriserver.domain.part.application.port.in.GetBrandsPartUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.GetPartInfosUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.RecordUsagePartUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.ResolvePartUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.SearchPartUseCase;
import ijiri.ijiriserver.domain.part.application.port.in.SuggestPartUseCase;
import ijiri.ijiriserver.domain.part.application.port.out.BrandPort;
import ijiri.ijiriserver.domain.part.application.port.out.PartCarModelUsagePort;
import ijiri.ijiriserver.domain.part.application.port.out.PartPort;
import ijiri.ijiriserver.domain.part.entity.Brand;
import ijiri.ijiriserver.domain.part.entity.Part;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.part.exception.PartStatusCode;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
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
public class PartService implements SearchPartUseCase,
        SuggestPartUseCase,
        GetBrandsPartUseCase,
        ResolvePartUseCase,
        GetPartInfosUseCase,
        RecordUsagePartUseCase {

    private static final int SUGGEST_LIMIT = 3;
    // 이보다 낮은 유사도는 "혹시 이 부품인가요?" 후보로 보여주지 않는다
    private static final double MIN_SUGGEST_SCORE = 0.3;
    private static final int BRAND_LIMIT = 30;

    private final PartPort partPort;
    private final BrandPort brandPort;
    private final PartCarModelUsagePort partCarModelUsagePort;
    private final Clock clock;

    @Override
    public PartResponse search(String keyword, PartCategory category, Long carModelId, int size) {
        List<Part> parts = partPort.search(
                containsPattern(keyword),
                category != null ? category.name() : "",
                carModelId != null ? carModelId : 0L,
                size
        );
        return PartResponse.parts(parts, toInfos(parts));
    }

    // 키워드·별칭 부분 일치와 trigram 유사도를 합쳐 후보 3개. 임베딩(벡터) 검색은 아직 붙이지 않았다
    @Override
    public PartResponse suggest(String keyword, PartCategory category, Long carModelId) {
        List<PartSuggestion> found = partPort.suggest(
                normalize(keyword),
                containsPattern(keyword),
                category != null ? category.name() : "",
                carModelId != null ? carModelId : 0L,
                SUGGEST_LIMIT
        ).stream()
                .filter(suggestion -> suggestion.getScore() >= MIN_SUGGEST_SCORE)
                .toList();
        Map<Long, PartInfo> infos = getPartInfos(found.stream().map(PartSuggestion::getId).toList());
        Map<Long, Double> scores = found.stream()
                .collect(Collectors.toMap(PartSuggestion::getId, suggestion -> round(suggestion.getScore())));
        return PartResponse.suggestions(
                found.stream().map(suggestion -> infos.get(suggestion.getId())).toList(),
                scores
        );
    }

    @Override
    public PartResponse getBrands(String keyword) {
        return PartResponse.brands(brandPort.findByNormalizedNameContainingOrderByNameAsc(
                normalize(keyword),
                Limit.of(BRAND_LIMIT)
        ));
    }

    @Override
    @Transactional
    public PartInfo resolve(PartCommand command) {
        if (command.partId() != null) {
            Part part = partPort.findById(command.partId())
                    .orElseThrow(() -> new CustomException(PartStatusCode.PART_NOT_FOUND));
            return PartInfo.of(part, findBrand(part.getBrandId()));
        }
        if (!StringUtils.hasText(command.partName()) || command.category() == null) {
            throw new CustomException(PartStatusCode.INVALID_PART);
        }
        Brand brand = StringUtils.hasText(command.brandName()) ? findOrCreateBrand(command.brandName()) : null;
        return PartInfo.of(findOrCreatePart(brand, command), brand);
    }

    @Override
    public Map<Long, PartInfo> getPartInfos(Collection<Long> partIds) {
        return toInfos(partPort.findAllById(partIds));
    }

    @Override
    @Transactional
    public void recordUsage(Collection<Long> partIds, Long carModelId, int delta) {
        if (partIds.isEmpty()) {
            return;
        }
        partPort.addUseCount(partIds, delta);
        partIds.forEach(partId -> partCarModelUsagePort.add(partId, carModelId, delta));
    }

    // 정규화한 이름이 완전히 같을 때만 같은 부품으로 보고, 분류는 처음 등록된 값을 따른다
    private Part findOrCreatePart(Brand brand, PartCommand command) {
        String name = cleanName(command.partName());
        String normalizedName = normalize(name);
        LocalDateTime now = LocalDateTime.now(clock);
        if (brand != null) {
            partPort.insertIfAbsent(brand.getId(), name, normalizedName, command.category().name(), now);
            return partPort.findByBrandIdAndNormalizedName(brand.getId(), normalizedName).orElseThrow();
        }
        partPort.insertWithoutBrandIfAbsent(name, normalizedName, command.category().name(), now);
        return partPort.findByBrandIdIsNullAndNormalizedName(normalizedName).orElseThrow();
    }

    private Brand findOrCreateBrand(String brandName) {
        String name = cleanName(brandName);
        String normalizedName = normalize(name);
        brandPort.insertIfAbsent(name, normalizedName, LocalDateTime.now(clock));
        return brandPort.findByNormalizedName(normalizedName).orElseThrow();
    }

    private Brand findBrand(Long brandId) {
        return brandId == null ? null : brandPort.findById(brandId).orElse(null);
    }

    private Map<Long, PartInfo> toInfos(List<Part> parts) {
        List<Long> brandIds = parts.stream()
                .map(Part::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Brand> brands = brandPort.findAllById(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));
        return parts.stream()
                .map(part -> PartInfo.of(part, part.getBrandId() != null ? brands.get(part.getBrandId()) : null))
                .collect(Collectors.toMap(PartInfo::id, Function.identity()));
    }

    // 화면에 보일 이름: 앞뒤 공백 제거, 연속 공백은 하나로
    private String cleanName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    // 같은 부품 판단과 검색 기준: 대소문자, 띄어쓰기, 하이픈 차이를 무시한다 ("te-37" = "TE37")
    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "");
    }

    private String containsPattern(String keyword) {
        String escaped = normalize(keyword).replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private double round(double score) {
        return Math.round(score * 100) / 100.0;
    }
}

package ijiri.ijiriserver.domain.part.controller;

import ijiri.ijiriserver.domain.part.dto.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.PartCategory;
import ijiri.ijiriserver.domain.part.service.PartService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Part", description = "부품 / 브랜드 검색")
@RestController
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @Operation(
            summary = "부품 자동완성",
            description = "부품 태그 입력용. 부품명·브랜드명 부분 일치, 그다음 부품명 유사도(오타 허용) 순으로 20개"
    )
    @GetMapping("/parts/suggest")
    public BaseResponse<PartResponse> suggest(
            @RequestParam @NotBlank @Size(max = 100) String keyword,
            @RequestParam(required = false) PartCategory category
    ) {
        return BaseResponse.ok(partService.suggest(keyword, category));
    }

    @Operation(
            summary = "부품 목록",
            description = "부품명, 분류, 브랜드로 거른다. 최신 등록순, cursor 는 이전 응답의 nextCursor"
    )
    @GetMapping("/parts")
    public BaseResponse<PartResponse> getParts(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) PartCategory category,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(partService.getParts(keyword, category, brandId, cursor, size));
    }

    @Operation(
            summary = "브랜드 검색",
            description = "브랜드명 부분 일치, 이름순 30개. keyword 를 비우면 이름순 앞에서부터"
    )
    @GetMapping("/brands")
    public BaseResponse<PartResponse> getBrands(
            @RequestParam(required = false) @Size(max = 50) String keyword
    ) {
        return BaseResponse.ok(partService.getBrands(keyword));
    }
}

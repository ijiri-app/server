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
            summary = "부품 검색",
            description = "부품명·브랜드명·별칭 부분 일치(대소문자·띄어쓰기·하이픈 무시). "
                    + "carModelId 를 주면 그 차종에 자주 달린 부품이 위로 온다"
    )
    @GetMapping("/parts")
    public BaseResponse<PartResponse> search(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) PartCategory category,
            @RequestParam(required = false) Long carModelId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(partService.search(q, category, carModelId, size));
    }

    @Operation(
            summary = "부품 후보 추천",
            description = "직접 입력한 이름과 같은 부품일 수 있는 후보 최대 3개와 score(0~1). "
                    + "고르면 기존 partId 로, 안 고르면 새 부품(확인 대기)으로 올린다. 자동으로 합치지 않는다"
    )
    @GetMapping("/parts/suggest")
    public BaseResponse<PartResponse> suggest(
            @RequestParam @NotBlank @Size(max = 100) String q,
            @RequestParam(required = false) PartCategory category,
            @RequestParam(required = false) Long carModelId
    ) {
        return BaseResponse.ok(partService.suggest(q, category, carModelId));
    }

    @Operation(
            summary = "브랜드 검색",
            description = "브랜드명 부분 일치(대소문자·띄어쓰기·하이픈 무시), 이름순 30개"
    )
    @GetMapping("/brands")
    public BaseResponse<PartResponse> getBrands(
            @RequestParam(required = false) @Size(max = 50) String q
    ) {
        return BaseResponse.ok(partService.getBrands(q));
    }
}

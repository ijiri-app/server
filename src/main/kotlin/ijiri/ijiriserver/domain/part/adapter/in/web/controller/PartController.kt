package ijiri.ijiriserver.domain.part.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse
import ijiri.ijiriserver.domain.part.application.port.`in`.GetBrandsUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.SearchPartsUseCase
import ijiri.ijiriserver.domain.part.application.port.`in`.SuggestPartsUseCase
import ijiri.ijiriserver.domain.part.entity.PartCategory
import ijiri.ijiriserver.global.data.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Part", description = "부품 / 브랜드 검색")
@RestController
class PartController(
    private val getBrandsUseCase: GetBrandsUseCase,
    private val searchPartsUseCase: SearchPartsUseCase,
    private val suggestPartsUseCase: SuggestPartsUseCase,
) {

    @Operation(
        summary = "부품 검색",
        description = "부품명·브랜드명·별칭 부분 일치(대소문자·띄어쓰기·하이픈 무시). " +
            "carModelId 를 주면 그 차종에 자주 달린 부품이 위로 온다",
    )
    @GetMapping("/parts")
    fun search(
        @RequestParam(required = false) @Size(max = 100) q: String?,
        @RequestParam(required = false) category: PartCategory?,
        @RequestParam(required = false) carModelId: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): BaseResponse<PartResponse> {
        return BaseResponse.ok(searchPartsUseCase.search(q, category, carModelId, size))
    }

    @Operation(
        summary = "부품 후보 추천",
        description = "직접 입력한 이름과 같은 부품일 수 있는 후보 최대 3개와 score(0~1). " +
            "고르면 기존 partId 로, 안 고르면 새 부품(확인 대기)으로 올린다. 자동으로 합치지 않는다",
    )
    @GetMapping("/parts/suggest")
    fun suggest(
        @RequestParam @NotBlank @Size(max = 100) q: String,
        @RequestParam(required = false) category: PartCategory?,
        @RequestParam(required = false) carModelId: Long?,
    ): BaseResponse<PartResponse> {
        return BaseResponse.ok(suggestPartsUseCase.suggest(q, category, carModelId))
    }

    @Operation(
        summary = "브랜드 검색",
        description = "브랜드명 부분 일치(대소문자·띄어쓰기·하이픈 무시), 이름순 30개",
    )
    @GetMapping("/brands")
    fun getBrands(
        @RequestParam(required = false) @Size(max = 50) q: String?,
    ): BaseResponse<PartResponse> {
        return BaseResponse.ok(getBrandsUseCase.getBrands(q))
    }
}

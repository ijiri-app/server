package ijiri.ijiriserver.domain.interestcar.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.interestcar.application.data.request.InterestCarUpdateRequest
import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.GetInterestCarsUseCase
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.ReplaceInterestCarsUseCase
import ijiri.ijiriserver.global.data.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "InterestCar", description = "관심 차종")
@RestController
@RequestMapping("/members/me/interest-cars")
class InterestCarController(
    private val getInterestCarsUseCase: GetInterestCarsUseCase,
    private val replaceInterestCarsUseCase: ReplaceInterestCarsUseCase,
) {

    @Operation(
        summary = "관심 차종 조회",
        description = "화면에 보이는 순서대로 반환",
    )
    @GetMapping
    fun getAll(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
    ): BaseResponse<InterestCarResponse> {
        return BaseResponse.ok(getInterestCarsUseCase.getAll(memberId.toLong()))
    }

    @Operation(
        summary = "관심 차종 저장",
        description = "전체 교체(0~30개, 배열 순서 = 피드 탭 순서). 신규 가입 직후 기기에 저장해 둔 목록을 올리거나 " +
            "편집 화면에서 사용. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping
    fun replaceAll(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: InterestCarUpdateRequest,
    ) {
        replaceInterestCarsUseCase.replaceAll(memberId.toLong(), request.carModelIds!!)
    }
}

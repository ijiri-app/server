package ijiri.ijiriserver.domain.interestcar.controller;

import ijiri.ijiriserver.domain.interestcar.dto.request.InterestCarUpdateRequest;
import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;
import ijiri.ijiriserver.domain.interestcar.service.InterestCarService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "InterestCar", description = "관심 차종")
@RestController
@RequestMapping("/members/me/interest-cars")
@RequiredArgsConstructor
public class InterestCarController {

    private final InterestCarService interestCarService;

    @Operation(
            summary = "관심 차종 조회",
            description = "화면에 보이는 순서대로 반환"
    )
    @GetMapping
    public BaseResponse<InterestCarResponse> getAll(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId
    ) {
        return BaseResponse.ok(interestCarService.getAll(Long.valueOf(memberId)));
    }

    @Operation(
            summary = "관심 차종 저장",
            description = "전체 교체(0~30개, 배열 순서 = 피드 탭 순서). 신규 가입 직후 기기에 저장해 둔 목록을 올리거나 "
                    + "편집 화면에서 사용. 204"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping
    public void replaceAll(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody InterestCarUpdateRequest request
    ) {
        interestCarService.replaceAll(Long.valueOf(memberId), request.carModelIds());
    }
}

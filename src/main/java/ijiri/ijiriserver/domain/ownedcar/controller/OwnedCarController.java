package ijiri.ijiriserver.domain.ownedcar.controller;

import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarUpdateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.response.OwnedCarResponse;
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OwnedCar", description = "보유 차량")
@RestController
@RequestMapping("/members/me/cars")
@RequiredArgsConstructor
public class OwnedCarController {

    private final OwnedCarService ownedCarService;

    @Operation(
            summary = "보유 차량 목록"
    )
    @GetMapping
    public BaseResponse<OwnedCarResponse> getAll(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId
    ) {
        return BaseResponse.ok(ownedCarService.getAll(Long.valueOf(memberId)));
    }

    @Operation(
            summary = "보유 차량 등록",
            description = "모델·세대·트림(선택), 연식, 빌드 방향. 최대 10대"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BaseResponse<OwnedCarResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody OwnedCarCreateRequest request
    ) {
        return BaseResponse.of(
                OwnedCarStatusCode.CREATE_SUCCESS,
                ownedCarService.create(Long.valueOf(memberId), request)
        );
    }

    @Operation(
            summary = "보유 차량 수정",
            description = "모든 항목을 요청 값으로 바꾼다"
    )
    @PatchMapping("/{ownedCarId}")
    public BaseResponse<OwnedCarResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long ownedCarId,
            @Valid @RequestBody OwnedCarUpdateRequest request
    ) {
        return BaseResponse.of(
                OwnedCarStatusCode.UPDATE_SUCCESS,
                ownedCarService.update(Long.valueOf(memberId), ownedCarId, request)
        );
    }

    @Operation(
            summary = "보유 차량 삭제",
            description = "이미 올린 게시물의 차량 정보는 그대로 남는다"
    )
    @DeleteMapping("/{ownedCarId}")
    public BaseResponse<OwnedCarResponse> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long ownedCarId
    ) {
        return BaseResponse.of(
                OwnedCarStatusCode.DELETE_SUCCESS,
                ownedCarService.delete(Long.valueOf(memberId), ownedCarId)
        );
    }
}

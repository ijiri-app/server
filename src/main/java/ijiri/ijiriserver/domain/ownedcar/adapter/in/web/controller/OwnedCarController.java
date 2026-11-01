package ijiri.ijiriserver.domain.ownedcar.adapter.in.web.controller;

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarUpdateRequest;
import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse;
import ijiri.ijiriserver.domain.ownedcar.application.port.in.CreateOwnedCarUseCase;
import ijiri.ijiriserver.domain.ownedcar.application.port.in.DeleteOwnedCarUseCase;
import ijiri.ijiriserver.domain.ownedcar.application.port.in.GetOwnedCarsUseCase;
import ijiri.ijiriserver.domain.ownedcar.application.port.in.UpdateOwnedCarUseCase;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode;
import ijiri.ijiriserver.global.data.BaseResponse;
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

    private final CreateOwnedCarUseCase createOwnedCarUseCase;
    private final DeleteOwnedCarUseCase deleteOwnedCarUseCase;
    private final GetOwnedCarsUseCase getOwnedCarsUseCase;
    private final UpdateOwnedCarUseCase updateOwnedCarUseCase;

    @Operation(
            summary = "보유 차량 목록",
            description = "지금 타는 차(OWNED) 다음 이전 차량(PAST). 차량별 게시물 수 포함"
    )
    @GetMapping
    public BaseResponse<OwnedCarResponse> getAll(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId
    ) {
        return BaseResponse.ok(getOwnedCarsUseCase.getAll(Long.valueOf(memberId)));
    }

    @Operation(
            summary = "보유 차량 등록",
            description = "트림, 연식(세대 판매 기간 안), 빌드 방향, 별칭. 최대 10대"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BaseResponse<OwnedCarResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody OwnedCarCreateRequest request
    ) {
        return BaseResponse.of(
                OwnedCarStatusCode.CREATE_SUCCESS,
                createOwnedCarUseCase.create(Long.valueOf(memberId), request)
        );
    }

    @Operation(
            summary = "보유 차량 수정",
            description = "연식, 빌드 방향, 별칭, 상태(OWNED/PAST). 보낸 필드만 바뀐다. 204"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{ownedCarId}")
    public void update(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long ownedCarId,
            @Valid @RequestBody OwnedCarUpdateRequest request
    ) {
        updateOwnedCarUseCase.update(Long.valueOf(memberId), ownedCarId, request);
    }

    @Operation(
            summary = "보유 차량 삭제",
            description = "게시물이 연결된 차량은 삭제 대신 이전 차량(PAST)으로 바뀐다. 204"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{ownedCarId}")
    public void delete(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long ownedCarId
    ) {
        deleteOwnedCarUseCase.delete(Long.valueOf(memberId), ownedCarId);
    }
}

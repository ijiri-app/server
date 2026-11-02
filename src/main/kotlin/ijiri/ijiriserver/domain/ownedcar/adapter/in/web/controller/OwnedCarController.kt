package ijiri.ijiriserver.domain.ownedcar.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarCreateRequest
import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarUpdateRequest
import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.CreateOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.DeleteOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.GetOwnedCarsUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.UpdateOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode
import ijiri.ijiriserver.global.data.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "OwnedCar", description = "보유 차량")
@RestController
@RequestMapping("/members/me/cars")
class OwnedCarController(
    private val createOwnedCarUseCase: CreateOwnedCarUseCase,
    private val deleteOwnedCarUseCase: DeleteOwnedCarUseCase,
    private val getOwnedCarsUseCase: GetOwnedCarsUseCase,
    private val updateOwnedCarUseCase: UpdateOwnedCarUseCase,
) {

    @Operation(
        summary = "보유 차량 목록",
        description = "지금 타는 차(OWNED) 다음 이전 차량(PAST). 차량별 게시물 수 포함",
    )
    @GetMapping
    fun getAll(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
    ): BaseResponse<OwnedCarResponse> {
        return BaseResponse.ok(getOwnedCarsUseCase.getAll(memberId.toLong()))
    }

    @Operation(
        summary = "보유 차량 등록",
        description = "트림, 연식(세대 판매 기간 안), 빌드 방향, 별칭. 최대 10대",
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: OwnedCarCreateRequest,
    ): BaseResponse<OwnedCarResponse> {
        return BaseResponse.of(
            OwnedCarStatusCode.CREATE_SUCCESS,
            createOwnedCarUseCase.create(memberId.toLong(), request),
        )
    }

    @Operation(
        summary = "보유 차량 수정",
        description = "연식, 빌드 방향, 별칭, 상태(OWNED/PAST). 보낸 필드만 바뀐다. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{ownedCarId}")
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @PathVariable ownedCarId: Long,
        @Valid @RequestBody request: OwnedCarUpdateRequest,
    ) {
        updateOwnedCarUseCase.update(memberId.toLong(), ownedCarId, request)
    }

    @Operation(
        summary = "보유 차량 삭제",
        description = "게시물이 연결된 차량은 삭제 대신 이전 차량(PAST)으로 바뀐다. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{ownedCarId}")
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @PathVariable ownedCarId: Long,
    ) {
        deleteOwnedCarUseCase.delete(memberId.toLong(), ownedCarId)
    }
}

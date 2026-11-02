package ijiri.ijiriserver.user.ownedcar.adapter.`in`.web.controller

import ijiri.ijiriserver.common.contract.OwnedCarSnapshot
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.GetOwnedCarSnapshotUseCase
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * service-community 가 게시물을 올릴 때 내 차량인지 확인하고 차종·연식을 복사해 가려고 부른다.
 */
@Hidden
@RestController
class OwnedCarInternalController(
    private val getOwnedCarSnapshotUseCase: GetOwnedCarSnapshotUseCase,
) {

    // 남의 차량이면 FORBIDDEN, 없으면 NOT_FOUND 를 그대로 돌려준다
    @GetMapping("/internal/owned-cars/{ownedCarId}")
    fun getSnapshot(@PathVariable ownedCarId: Long, @RequestParam memberId: Long): BaseResponse<OwnedCarSnapshot> =
        BaseResponse.ok(getOwnedCarSnapshotUseCase.getSnapshot(memberId, ownedCarId))
}

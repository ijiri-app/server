package ijiri.ijiriserver.user.interestcar.adapter.`in`.web.controller

import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.user.interestcar.application.port.`in`.GetInterestCarModelIdsUseCase
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * service-community 가 로그인한 회원의 기본 피드(관심 차종)를 만들 때 부른다.
 */
@Hidden
@RestController
class InterestCarInternalController(
    private val getInterestCarModelIdsUseCase: GetInterestCarModelIdsUseCase,
) {

    @GetMapping("/internal/members/{memberId}/interest-cars")
    fun getCarModelIds(@PathVariable memberId: Long): BaseResponse<List<Long>> =
        BaseResponse.ok(getInterestCarModelIdsUseCase.getCarModelIds(memberId))
}

package ijiri.ijiriserver.user.member.adapter.`in`.web.controller

import ijiri.ijiriserver.common.contract.MemberSummary
import ijiri.ijiriserver.common.contract.SuspendMemberRequest
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberSummaryUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.SuspendMemberUseCase
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * service-community 가 게시물 작성자, 차단 목록, 신고 대상 확인과 이용 정지에 내부망에서 부른다.
 */
@Hidden
@RestController
@RequestMapping("/internal/members")
class MemberInternalController(
    private val getMemberSummaryUseCase: GetMemberSummaryUseCase,
    private val suspendMemberUseCase: SuspendMemberUseCase,
) {

    @GetMapping("/{memberId}")
    fun getSummary(@PathVariable memberId: Long): BaseResponse<MemberSummary> =
        BaseResponse.ok(getMemberSummaryUseCase.getSummary(memberId))

    @GetMapping
    fun getSummaries(@RequestParam(required = false) ids: List<Long>?): BaseResponse<List<MemberSummary>> =
        BaseResponse.ok(getMemberSummaryUseCase.getSummaries(ids.orEmpty()))

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{memberId}/suspend")
    fun suspend(@PathVariable memberId: Long, @RequestBody request: SuspendMemberRequest) {
        suspendMemberUseCase.suspend(memberId, request.until)
    }
}

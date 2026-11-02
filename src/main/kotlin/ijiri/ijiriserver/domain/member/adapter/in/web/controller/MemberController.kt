package ijiri.ijiriserver.domain.member.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.member.application.data.request.MemberUpdateRequest
import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberProfileUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMyProfileUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.UpdateMemberProfileUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.WithdrawMemberUseCase
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode
import ijiri.ijiriserver.global.data.BaseResponse
import ijiri.ijiriserver.global.jwt.JwtCookieManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/members")
class MemberController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getMemberProfileUseCase: GetMemberProfileUseCase,
    private val updateMemberProfileUseCase: UpdateMemberProfileUseCase,
    private val withdrawMemberUseCase: WithdrawMemberUseCase,
    private val jwtCookieManager: JwtCookieManager,
) {

    @Operation(
        summary = "내 정보 조회",
        description = "프로필, 가입 수단, 이메일, 게시물 수, 받은 담기 수",
    )
    @GetMapping("/me")
    fun getMe(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
    ): BaseResponse<MemberResponse> {
        return BaseResponse.ok(getMyProfileUseCase.getMe(memberId.toLong()))
    }

    @Operation(
        summary = "내 프로필 수정",
        description = "닉네임(중복 불가), 프로필 사진(imageKey), 상태 메시지. 보낸 필드만 바뀐다",
    )
    @PatchMapping("/me")
    fun updateMe(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: MemberUpdateRequest,
    ): BaseResponse<MemberResponse> {
        return BaseResponse.of(
            MemberStatusCode.UPDATE_SUCCESS,
            updateMemberProfileUseCase.updateProfile(memberId.toLong(), request),
        )
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "즉시 세션 폐기, 카카오 연결 끊기, 프로필·게시물·위시리스트 비공개, 이메일 삭제. " +
            "30일 뒤 모든 데이터와 사진 파일을 완전히 삭제한다. 토큰 쿠키도 만료. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/me")
    fun withdraw(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        httpResponse: HttpServletResponse,
    ) {
        withdrawMemberUseCase.withdraw(memberId.toLong())
        jwtCookieManager.expireTokenCookies(httpResponse)
    }

    @Operation(
        summary = "다른 회원 프로필",
        description = "이메일·가입 수단은 빠진다",
    )
    @GetMapping("/{memberId}")
    fun getProfile(@PathVariable memberId: Long): BaseResponse<MemberResponse> {
        return BaseResponse.ok(getMemberProfileUseCase.getProfile(memberId))
    }
}

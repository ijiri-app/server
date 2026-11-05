package ijiri.ijiriserver.user.auth.token.adapter.`in`.web.controller

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.token.application.data.request.RefreshTokenRequest
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RefreshTokenUseCase
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.jwt.JwtCookieManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
class TokenController(
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val jwtCookieManager: JwtCookieManager,
) {

    @Operation(
        summary = "토큰 갱신",
        description = "body 의 refreshToken, 없으면 refreshToken 쿠키로 토큰을 새로 발급하고 쿠키에도 저장. " +
            "이미 교체됐거나 로그아웃·다른 기기 로그인으로 지워진 토큰은 거부(INVALID_REFRESH_TOKEN)",
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): BaseResponse<AuthResponse> {
        val refreshToken = jwtCookieManager.resolveRefreshToken(httpRequest, request?.refreshToken)
            ?: throw CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN)

        val tokens = refreshTokenUseCase.refresh(refreshToken)
        jwtCookieManager.addTokenCookies(httpResponse, tokens.accessToken!!, tokens.refreshToken!!)
        return BaseResponse.of(AuthStatusCode.REFRESH_SUCCESS, tokens)
    }
}

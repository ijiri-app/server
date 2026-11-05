package ijiri.ijiriserver.user.auth.oauth.adapter.`in`.web.controller

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.oauth.application.data.request.OAuthSignInRequest
import ijiri.ijiriserver.user.auth.oauth.application.port.`in`.OAuthSignInUseCase
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.common.jwt.JwtCookieManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
class OAuthController(
    private val oAuthSignInUseCase: OAuthSignInUseCase,
    private val jwtCookieManager: JwtCookieManager,
) {

    @Operation(
        summary = "소셜 로그인 (카카오/구글 공용)",
        description = "카카오는 SDK 의 accessToken, 구글은 idToken 을 token 에 담는다. " +
            "신규 회원이면 자동 가입(닉네임 자동 생성)하고 isNewMember = true",
    )
    @SecurityRequirements
    @PostMapping("/login")
    fun signIn(
        @Valid @RequestBody request: OAuthSignInRequest,
        httpResponse: HttpServletResponse,
    ): BaseResponse<AuthResponse> {
        val response = oAuthSignInUseCase.signIn(request)
        jwtCookieManager.addTokenCookies(httpResponse, response.accessToken!!, response.refreshToken!!)
        return BaseResponse.of(AuthStatusCode.SIGNIN_SUCCESS, response)
    }
}

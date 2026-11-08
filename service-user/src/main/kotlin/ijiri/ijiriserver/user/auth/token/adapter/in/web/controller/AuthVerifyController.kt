package ijiri.ijiriserver.user.auth.token.adapter.`in`.web.controller

import ijiri.ijiriserver.common.jwt.JwtCookieManager
import ijiri.ijiriserver.common.security.GatewayHeaders
import ijiri.ijiriserver.user.auth.token.application.port.`in`.VerifyAccessTokenUseCase
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * nginx 의 auth_request 가 모든 요청마다 부른다. 유효한 access token(헤더 또는 쿠키)이면 회원 id 와 DB 의 역할을
 * 응답 헤더에 실어 주고 nginx 가 그 값을 X-Member-Id, X-Member-Role 로 서비스에 넘긴다.
 * 토큰이 없거나 유효하지 않아도 200 이다. 공개 API 는 비로그인 요청으로 처리되고, 보호된 API 는 각 서비스가 401 로 거절한다.
 */
@Hidden
@RestController
class AuthVerifyController(
    private val verifyAccessTokenUseCase: VerifyAccessTokenUseCase,
    private val jwtCookieManager: JwtCookieManager,
) {

    @GetMapping("/internal/auth/verify")
    fun verify(request: HttpServletRequest): ResponseEntity<Void> {
        val verified = jwtCookieManager.resolveAccessToken(request)
            ?.let { verifyAccessTokenUseCase.verify(it) }
            ?: return ResponseEntity.ok().build()
        return ResponseEntity.ok()
            .header(GatewayHeaders.MEMBER_ID, verified.memberId.toString())
            .header(GatewayHeaders.MEMBER_ROLE, verified.role.name)
            .build()
    }
}

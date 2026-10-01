package ijiri.ijiriserver.domain.auth.token.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final JwtCookieManager jwtCookieManager;

    @Operation(
            summary = "토큰 갱신",
            description = "body 의 refreshToken, 없으면 refreshToken 쿠키로 토큰을 새로 발급하고 쿠키에도 저장. "
                    + "이미 교체됐거나 로그아웃·다른 기기 로그인으로 지워진 토큰은 거부(INVALID_REFRESH_TOKEN)"
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    public BaseResponse<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String bodyToken = request == null ? null : request.refreshToken();
        String refreshToken = jwtCookieManager.resolveRefreshToken(httpRequest, bodyToken)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));

        AuthResponse tokens = tokenService.refresh(refreshToken);
        jwtCookieManager.addTokenCookies(httpResponse, tokens.accessToken(), tokens.refreshToken());
        return BaseResponse.of(AuthStatusCode.REFRESH_SUCCESS, tokens);
    }
}

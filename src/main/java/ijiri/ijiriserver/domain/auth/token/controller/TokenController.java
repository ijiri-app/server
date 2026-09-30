package ijiri.ijiriserver.domain.auth.token.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

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
                    + "이미 교체된 토큰이 다시 오면 탈취로 보고 회원의 모든 refresh token 을 폐기"
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    public BaseResponse<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = Optional.ofNullable(request)
                .map(RefreshTokenRequest::refreshToken)
                .filter(StringUtils::hasText)
                .or(() -> jwtCookieManager.resolveRefreshToken(httpRequest))
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));

        AuthResponse tokens = tokenService.refresh(refreshToken);
        jwtCookieManager.addTokenCookies(httpResponse, tokens.accessToken(), tokens.refreshToken());
        return BaseResponse.of(AuthStatusCode.REFRESH_SUCCESS, tokens);
    }

    @Operation(
            summary = "로그아웃",
            description = "Authorization 헤더 또는 accessToken 쿠키의 회원을 로그아웃. "
                    + "토큰 쿠키를 만료시키고 refresh token 을 모두 삭제(모든 기기 로그아웃)"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = tokenService.signOut(Long.valueOf(memberId));
        jwtCookieManager.expireTokenCookies(httpResponse);
        return BaseResponse.of(AuthStatusCode.SIGNOUT_SUCCESS, response);
    }
}

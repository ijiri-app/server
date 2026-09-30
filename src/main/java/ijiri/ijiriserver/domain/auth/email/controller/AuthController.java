package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    // 여러 계정을 돌려가며 대입하는 공격을 막는 IP 단위 제한 (계정 단위 제한은 서비스에서)
    private static final String SIGNIN_KEY_PREFIX = "signin:ip:";
    private static final int SIGNIN_LIMIT_PER_IP = 30;
    private static final Duration SIGNIN_LIMIT_PERIOD = Duration.ofMinutes(15);

    private final AuthService authService;
    private final JwtCookieManager jwtCookieManager;
    private final RateLimiter rateLimiter;

    @Operation(
            summary = "이메일 회원가입",
            description = "인증 코드 확인을 마친 이메일 + 비밀번호, 닉네임으로 가입. "
                    + "가입과 동시에 로그인되어 토큰을 body 와 쿠키로 발급한다"
    )
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public BaseResponse<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.signup(request);
        jwtCookieManager.addTokenCookies(httpResponse, response.accessToken(), response.refreshToken());
        return BaseResponse.of(AuthStatusCode.SIGNUP_SUCCESS, response);
    }

    @Operation(
            summary = "이메일 로그인",
            description = "이메일, 비밀번호로 로그인. 계정/IP 별로 15분당 시도 횟수 제한"
    )
    @SecurityRequirements
    @PostMapping("/signin")
    public BaseResponse<AuthResponse> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        rateLimiter.check(SIGNIN_KEY_PREFIX + httpRequest.getRemoteAddr(), SIGNIN_LIMIT_PER_IP, SIGNIN_LIMIT_PERIOD);
        AuthResponse response = authService.signIn(request);
        jwtCookieManager.addTokenCookies(httpResponse, response.accessToken(), response.refreshToken());
        return BaseResponse.of(AuthStatusCode.SIGNIN_SUCCESS, response);
    }

    @Operation(
            summary = "로그아웃",
            description = "유효한 access token(헤더/쿠키)이 있으면 그 회원의 세션을, "
                    + "access token 이 만료됐으면 body 또는 쿠키의 refreshToken 세션을 지운다. 토큰 쿠키도 만료"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = memberId != null
                ? authService.signOut(Long.valueOf(memberId))
                : authService.signOutByRefreshToken(resolveRefreshToken(request, httpRequest));
        jwtCookieManager.expireTokenCookies(httpResponse);
        return BaseResponse.of(AuthStatusCode.SIGNOUT_SUCCESS, response);
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String bodyToken = request == null ? null : request.refreshToken();
        return jwtCookieManager.resolveRefreshToken(httpRequest, bodyToken)
                .orElseThrow(() -> new CustomException(CommonStatusCode.UNAUTHORIZED));
    }
}

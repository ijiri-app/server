package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.PasswordResetRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
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
            description = "이메일, 비밀번호로 로그인. 오류는 AUTH4013 하나로 응답. "
                    + "이메일별 15분 안에 5회 실패하면 잠금(AUTH4293), IP 별 15분당 30회 제한"
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
            summary = "비밀번호 재설정",
            description = "RESET_PASSWORD 용도로 인증 코드 확인을 마친 이메일의 비밀번호를 바꾸고 모든 세션을 끊는다"
    )
    @SecurityRequirements
    @PostMapping("/password/reset")
    public BaseResponse<AuthResponse> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return BaseResponse.of(AuthStatusCode.PASSWORD_RESET, authService.resetPassword(request));
    }

    @Operation(
            summary = "로그아웃",
            description = "유효한 access token(헤더/쿠키)이 있으면 그 회원의 세션을, 없으면 body(앱) 또는 쿠키(웹)의 "
                    + "refreshToken 세션을 지우고 토큰 쿠키를 만료시킨다. 토큰이 하나도 없으면 로그인 상태가 아니므로 401"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        // 두 경우 모두 세션을 먼저 지운 뒤 쿠키를 만료시킨다 (삭제가 실패하면 쿠키도 그대로 둔다)
        AuthResponse response = memberId != null
                ? authService.signOut(Long.valueOf(memberId))
                : authService.signOutByRefreshToken(resolveRefreshToken(request, httpRequest));
        jwtCookieManager.expireTokenCookies(httpResponse);
        return BaseResponse.of(AuthStatusCode.SIGNOUT_SUCCESS, response);
    }

    // 토큰이 하나도 없으면 로그인 상태가 아니므로 갱신 API 와 같은 코드(AUTH4012)로 거부한다.
    // refresh token 이 있으면 이미 만료·삭제된 세션이어도 남은 쿠키를 지우도록 성공으로 응답한다
    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String bodyToken = request == null ? null : request.refreshToken();
        return jwtCookieManager.resolveRefreshToken(httpRequest, bodyToken)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));
    }
}

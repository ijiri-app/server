package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.PasswordResetRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.VerificationCodeSendRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
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
@SecurityRequirements
public class AuthController {

    // 여러 계정을 돌려가며 대입하는 공격을 막는 IP 단위 제한 (계정 단위 잠금은 서비스에서)
    private static final String SIGNIN_KEY_PREFIX = "signin:ip:";
    private static final int SIGNIN_LIMIT_PER_IP = 30;
    private static final Duration SIGNIN_LIMIT_PERIOD = Duration.ofMinutes(15);
    // 이메일 주소를 바꿔가며 메일을 대량 발송시키는 것을 막는 IP 단위 제한 (가입 코드 발송과 같은 기준)
    private static final String SEND_KEY_PREFIX = "verification:ip:";
    private static final int SEND_LIMIT_PER_IP = 10;
    private static final Duration SEND_LIMIT_PERIOD = Duration.ofHours(1);

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final JwtCookieManager jwtCookieManager;
    private final RateLimiter rateLimiter;

    @Operation(
            summary = "이메일 회원가입",
            description = "인증 코드 확인으로 받은 verificationToken, 비밀번호, 닉네임(중복 불가), 필수 약관 3개. "
                    + "가입과 동시에 로그인되어 로그인 응답과 같은 형태로 토큰을 body 와 쿠키로 발급한다"
    )
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
            description = "오류는 INVALID_CREDENTIALS 하나로 응답. 이메일별 15분 안에 5번 실패하면 "
                    + "ACCOUNT_LOCKED(423, retryAfterSeconds). IP 별 15분당 30회 제한"
    )
    @PostMapping("/login/email")
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
            summary = "비밀번호 재설정 코드 발송",
            description = "숫자 6자리, 5분 유효, 같은 이메일 1분에 1번. 가입 여부와 무관하게 204 를 주고, "
                    + "활성 이메일 회원에게만 메일을 보낸다"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/password/reset/code")
    public void sendResetCode(
            @Valid @RequestBody VerificationCodeSendRequest request,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.check(SEND_KEY_PREFIX + httpRequest.getRemoteAddr(), SEND_LIMIT_PER_IP, SEND_LIMIT_PERIOD);
        emailVerificationService.sendResetCode(request.email());
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "메일로 받은 코드와 새 비밀번호. 코드가 틀리면 INVALID_VERIFICATION_CODE + remainingAttempts. "
                    + "바꾸면 모든 세션을 끊는다. 204"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/password/reset")
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }

    @Operation(
            summary = "로그아웃",
            description = "유효한 access token(헤더/쿠키)이 있으면 그 회원의 세션을, 없으면 body(앱) 또는 쿠키(웹)의 "
                    + "refreshToken 세션을 지우고 토큰 쿠키를 만료시킨다. 204. 토큰이 하나도 없으면 401"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public void signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        // 두 경우 모두 세션을 먼저 지운 뒤 쿠키를 만료시킨다 (삭제가 실패하면 쿠키도 그대로 둔다)
        if (memberId != null) {
            authService.signOut(Long.valueOf(memberId));
        } else {
            authService.signOutByRefreshToken(resolveRefreshToken(request, httpRequest));
        }
        jwtCookieManager.expireTokenCookies(httpResponse);
    }

    // 토큰이 하나도 없으면 로그인 상태가 아니므로 갱신 API 와 같은 코드(INVALID_REFRESH_TOKEN)로 거부한다.
    // refresh token 이 있으면 이미 만료·삭제된 세션이어도 남은 쿠키를 지우도록 성공으로 응답한다
    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String bodyToken = request == null ? null : request.refreshToken();
        return jwtCookieManager.resolveRefreshToken(httpRequest, bodyToken)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));
    }
}

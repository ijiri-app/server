package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    // 여러 계정을 돌려가며 대입하는 공격을 막는 IP 단위 제한 (계정 단위 제한은 서비스에서)
    private static final String SIGNIN_KEY_PREFIX = "signin:ip:";
    private static final int SIGNIN_LIMIT_PER_IP = 30;
    private static final Duration SIGNIN_LIMIT_PERIOD = Duration.ofMinutes(15);

    private final AuthService authService;
    private final JwtCookieManager jwtCookieManager;
    private final RateLimiter rateLimiter;

    @Operation(
            summary = "이메일 회원가입",
            description = "닉네임, 이메일, 비밀번호, 인증 코드로 가입. 토큰은 발급하지 않는다"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public BaseResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return BaseResponse.of(AuthStatusCode.SIGNUP_SUCCESS, authService.signup(request));
    }

    @Operation(
            summary = "이메일 로그인",
            description = "이메일, 비밀번호로 로그인. 계정/IP 별로 15분당 시도 횟수 제한"
    )
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
}

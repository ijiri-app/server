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
            description = "Authorization 헤더 또는 accessToken 쿠키의 회원을 로그아웃. "
                    + "토큰 쿠키를 만료시키고 refresh token 을 모두 삭제(모든 기기 로그아웃)"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.signOut(Long.valueOf(memberId));
        jwtCookieManager.expireTokenCookies(httpResponse);
        return BaseResponse.of(AuthStatusCode.SIGNOUT_SUCCESS, response);
    }
}

package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
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

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtCookieManager jwtCookieManager;

    @Operation(
            summary = "이메일 회원가입",
            description = "닉네임, 이메일, 비밀번호, 인증 코드로 가입. 토큰은 발급하지 않는다"
    )
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public BaseResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return BaseResponse.of(AuthStatusCode.SIGNUP_SUCCESS, authService.signup(request));
    }

    @Operation(
            summary = "이메일 로그인",
            description = "이메일, 비밀번호로 로그인"
    )
    @SecurityRequirements
    @PostMapping("/signin")
    public BaseResponse<AuthResponse> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.signIn(request);
        jwtCookieManager.addTokenCookies(httpResponse, response.accessToken(), response.refreshToken());
        return BaseResponse.of(AuthStatusCode.SIGNIN_SUCCESS, response);
    }

    @Operation(
            summary = "로그아웃",
            description = "Authorization 헤더 또는 accessToken 쿠키의 회원을 로그아웃. 토큰 쿠키를 만료시키고 refresh token 을 모두 삭제"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return BaseResponse.of(AuthStatusCode.SIGNOUT_SUCCESS, authService.signOut(httpRequest, httpResponse));
    }
}

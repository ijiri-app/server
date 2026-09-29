package ijiri.ijiriserver.domain.auth.email.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.SignInResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(
            summary = "이메일 회원가입",
            description = "닉네임, 이메일, 비밀번호로 가입하고 바로 로그인 토큰을 발급"
    )
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public BaseResponse<SignInResponse> signup(@Valid @RequestBody SignupRequest request) {
        return BaseResponse.of(AuthStatusCode.SIGNUP_SUCCESS, authService.signup(request));
    }

    @Operation(
            summary = "이메일 로그인",
            description = "이메일, 비밀번호로 로그인"
    )
    @SecurityRequirements
    @PostMapping("/signin")
    public BaseResponse<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return BaseResponse.of(AuthStatusCode.SIGNIN_SUCCESS, authService.signIn(request));
    }
}

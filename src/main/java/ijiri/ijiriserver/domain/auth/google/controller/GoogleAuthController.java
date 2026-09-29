package ijiri.ijiriserver.domain.auth.google.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.google.dto.request.GoogleLoginRequest;
import ijiri.ijiriserver.domain.auth.google.service.GoogleLoginService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "소셜 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class GoogleAuthController {

    private final GoogleLoginService googleLoginService;

    @Operation(summary = "구글 로그인", description = "Google Sign-In SDK 의 ID token 으로 로그인/회원가입")
    @PostMapping("/google")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        return BaseResponse.of(AuthStatusCode.LOGIN_SUCCESS, googleLoginService.login(request.idToken()));
    }
}

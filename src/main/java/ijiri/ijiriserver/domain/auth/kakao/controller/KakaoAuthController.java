package ijiri.ijiriserver.domain.auth.kakao.controller;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.kakao.dto.request.KakaoLoginRequest;
import ijiri.ijiriserver.domain.auth.kakao.service.KakaoLoginService;
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
public class KakaoAuthController {

    private final KakaoLoginService kakaoLoginService;

    @Operation(summary = "카카오 로그인", description = "카카오 SDK 의 access token 으로 로그인/회원가입")
    @PostMapping("/kakao")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody KakaoLoginRequest request) {
        return BaseResponse.of(AuthStatusCode.LOGIN_SUCCESS, kakaoLoginService.login(request.accessToken()));
    }
}

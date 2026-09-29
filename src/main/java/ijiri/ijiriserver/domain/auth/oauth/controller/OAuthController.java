package ijiri.ijiriserver.domain.auth.oauth.controller;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthLoginRequest;
import ijiri.ijiriserver.domain.auth.oauth.dto.response.OAuthLoginResponse;
import ijiri.ijiriserver.domain.auth.oauth.service.OAuthService;
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
public class OAuthController {

    private final OAuthService oAuthService;

    @Operation(
            summary = "소셜 로그인 (카카오/구글 공용)",
            description = "카카오는 SDK 의 accessToken, 구글은 idToken 을 token 에 담는다. 신규 회원이면 자동 가입"
    )
    @SecurityRequirements
    @PostMapping("/login")
    public BaseResponse<OAuthLoginResponse> login(@Valid @RequestBody OAuthLoginRequest request) {
        return BaseResponse.of(AuthStatusCode.LOGIN_SUCCESS, oAuthService.login(request));
    }
}

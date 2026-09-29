package ijiri.ijiriserver.domain.auth.token.controller;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.service.LogoutService;
import ijiri.ijiriserver.domain.auth.token.service.TokenReissueService;
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
public class TokenController {

    private final TokenReissueService tokenReissueService;
    private final LogoutService logoutService;

    @Operation(summary = "토큰 재발급", description = "자동 로그인. refresh token 으로 access/refresh token 을 새로 발급 (기존 refresh token 폐기)")
    @PostMapping("/reissue")
    public BaseResponse<TokenResponse> reissue(@Valid @RequestBody RefreshTokenRequest request) {
        return BaseResponse.of(AuthStatusCode.REISSUE_SUCCESS, tokenReissueService.reissue(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "해당 기기의 refresh token 폐기")
    @PostMapping("/logout")
    public BaseResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutService.logout(request.refreshToken());
        return BaseResponse.of(AuthStatusCode.LOGOUT_SUCCESS, null);
    }
}

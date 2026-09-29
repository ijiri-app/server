package ijiri.ijiriserver.domain.auth.token.controller;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.dto.request.RefreshTokenRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @Operation(
            summary = "토큰 갱신",
            description = "refresh token 으로 access/refresh token 을 모두 새로 발급하고 기존 refresh token 은 폐기"
    )
    @SecurityRequirements
    @PostMapping("/refresh")
    public BaseResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return BaseResponse.of(AuthStatusCode.REFRESH_SUCCESS, tokenService.refresh(request.refreshToken()));
    }

    @Operation(
            summary = "로그아웃",
            description = "해당 기기의 refresh token 폐기"
    )
    @PostMapping("/signout")
    public BaseResponse<AuthResponse> signOut(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return BaseResponse.of(
                AuthStatusCode.SIGNOUT_SUCCESS,
                tokenService.signOut(Long.valueOf(memberId), request.refreshToken())
        );
    }
}

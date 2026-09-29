package ijiri.ijiriserver.domain.auth.oauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthSignInRequest(
        @Schema(description = "로그인 제공자", example = "KAKAO", allowableValues = {"KAKAO", "GOOGLE"})
        @NotBlank String provider,

        @Schema(description = "카카오: SDK 의 accessToken / 구글: idToken")
        @NotBlank String token
) {
}

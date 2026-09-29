package ijiri.ijiriserver.domain.auth.kakao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @Schema(description = "카카오 SDK 가 발급한 access token")
        @NotBlank String accessToken
) {
}

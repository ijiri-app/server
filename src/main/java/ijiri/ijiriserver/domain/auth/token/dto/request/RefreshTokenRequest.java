package ijiri.ijiriserver.domain.auth.token.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenRequest(
        @Schema(description = "앱은 body 로 보낸다. 비우면 refreshToken 쿠키를 사용")
        String refreshToken
) {
}

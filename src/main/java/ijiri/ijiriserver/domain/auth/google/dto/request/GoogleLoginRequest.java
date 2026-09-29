package ijiri.ijiriserver.domain.auth.google.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @Schema(description = "Google Sign-In SDK 가 발급한 ID token")
        @NotBlank String idToken
) {
}

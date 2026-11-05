package ijiri.ijiriserver.user.auth.token.application.data.request

import io.swagger.v3.oas.annotations.media.Schema

data class RefreshTokenRequest(
    @Schema(description = "앱은 body 로 보낸다. 비우면 refreshToken 쿠키를 사용")
    val refreshToken: String?,
)

package ijiri.ijiriserver.user.auth.oauth.application.data.request

import ijiri.ijiriserver.user.member.entity.Provider
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class OAuthSignInRequest(
    @Schema(description = "로그인 제공자", example = "KAKAO", allowableValues = ["KAKAO", "GOOGLE"])
    @field:NotNull val provider: Provider?,

    @Schema(description = "카카오: SDK 의 accessToken / 구글: idToken")
    @field:NotBlank val token: String?,
)

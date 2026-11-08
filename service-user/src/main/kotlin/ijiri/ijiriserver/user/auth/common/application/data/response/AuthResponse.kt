package ijiri.ijiriserver.user.auth.common.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse

/**
 * auth 도메인의 모든 API 응답. 로그인·가입은 토큰 + isNewMember + member, 갱신은 토큰,
 * 인증 코드 확인은 verificationToken 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val accessTokenExpiresIn: Long?,
    val isNewMember: Boolean?,
    val member: MemberResponse?,
    val verificationToken: String?,
) {

    fun withSignIn(isNewMember: Boolean, member: MemberResponse): AuthResponse =
        AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, isNewMember, member, null)

    companion object {
        fun tokens(accessToken: String, refreshToken: String, accessTokenExpiresIn: Long): AuthResponse =
            AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, null, null, null)

        fun verified(verificationToken: String): AuthResponse =
            AuthResponse(null, null, null, null, null, verificationToken)
    }
}

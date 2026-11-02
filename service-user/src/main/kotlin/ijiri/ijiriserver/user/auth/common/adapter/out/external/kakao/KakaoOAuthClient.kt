package ijiri.ijiriserver.user.auth.common.adapter.out.external.kakao

import com.fasterxml.jackson.annotation.JsonProperty
import ijiri.ijiriserver.user.auth.common.application.port.out.SocialTokenVerifier
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.common.exception.CustomException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoOAuthClient(
    @Value("\${oauth.kakao.app-id}") private val appId: Long,
    externalApiRequestFactory: ClientHttpRequestFactory,
) : SocialTokenVerifier {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .requestFactory(externalApiRequestFactory)
        .build()

    override fun provider(): Provider = Provider.KAKAO

    override fun verify(token: String): MemberRegisterCommand {
        // 다른 앱에서 발급된 카카오 토큰으로 로그인하는 걸 막기 위해 app_id 확인
        val tokenInfo = get("/v1/user/access_token_info", token, TokenInfo::class.java)
        if (tokenInfo.appId != appId) {
            throw CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN)
        }

        val user = get("/v2/user/me", token, KakaoUser::class.java)
        val account = user.kakaoAccount
        val profile = account?.profile
        return MemberRegisterCommand(
            Provider.KAKAO,
            user.id.toString(),
            resolveEmail(account),
            profile?.nickname,
            if (profile != null && !profile.isDefaultImage) profile.profileImageUrl else null,
        )
    }

    // 카카오가 인증을 확인한 이메일만 사용한다 (미동의 또는 미인증이면 null)
    private fun resolveEmail(account: KakaoAccount?): String? {
        if (account == null || !account.isEmailValid || !account.isEmailVerified) {
            return null
        }
        return account.email
    }

    private fun <T : Any> get(uri: String, accessToken: String, type: Class<T>): T {
        try {
            return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                // 토큰이 잘못됐으면 400/401. 그 외(429 요청 제한 등)는 카카오 쪽 문제로 보고 아래에서 502 로 처리
                .onStatus({ it.value() == 400 || it.value() == 401 }) { _, _ ->
                    throw CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN)
                }
                .body(type)
                ?: throw CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR)
        } catch (e: RestClientException) {
            throw CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR)
        }
    }

    private data class TokenInfo(@JsonProperty("app_id") val appId: Long = 0)

    private data class KakaoUser(
        val id: Long = 0,
        @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
    )

    private data class KakaoAccount(
        val email: String?,
        @JsonProperty("is_email_valid") val isEmailValid: Boolean = false,
        @JsonProperty("is_email_verified") val isEmailVerified: Boolean = false,
        val profile: Profile?,
    )

    private data class Profile(
        val nickname: String?,
        @JsonProperty("profile_image_url") val profileImageUrl: String?,
        @JsonProperty("is_default_image") val isDefaultImage: Boolean = false,
    )

    companion object {
        private const val BASE_URL = "https://kapi.kakao.com"
    }
}

package ijiri.ijiriserver.domain.auth.common.adapter.out.external.google

import ijiri.ijiriserver.domain.auth.common.application.port.out.SocialTokenVerifier
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.domain.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.domain.member.entity.Provider
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Google ID token 을 Google 공개키(JWKS)로 서명 검증하고 iss, aud, exp 를 확인한다.
 */
@Component
class GoogleOAuthClient(
    @Value("\${oauth.google.client-ids}") clientIds: List<String>,
    externalApiRequestFactory: ClientHttpRequestFactory,
) : SocialTokenVerifier {

    private val jwtDecoder: JwtDecoder

    init {
        val decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
            .restOperations(RestTemplate(externalApiRequestFactory))
            .build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtTimestampValidator(),
                JwtClaimValidator<String?>(JwtClaimNames.ISS) { it in ISSUERS },
                JwtClaimValidator<List<String>?>(JwtClaimNames.AUD) { aud ->
                    aud != null && aud.any { it in clientIds }
                },
            )
        )
        jwtDecoder = decoder
    }

    override fun provider(): Provider = Provider.GOOGLE

    override fun verify(token: String): MemberRegisterCommand {
        val jwt = decode(token)
        // 구글이 소유를 확인하지 않은 이메일은 믿을 수 없으므로 저장하지 않는다
        val email = if (jwt.getClaimAsBoolean("email_verified") == true) jwt.getClaimAsString("email") else null
        return MemberRegisterCommand(
            Provider.GOOGLE,
            jwt.subject,
            email,
            jwt.getClaimAsString("name"),
            jwt.getClaimAsString("picture"),
        )
    }

    // 토큰 자체가 잘못된 경우와 JWKS 조회 실패(구글 서버 문제)를 구분한다
    private fun decode(idToken: String): Jwt {
        try {
            return jwtDecoder.decode(idToken)
        } catch (e: BadJwtException) {
            throw CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN)
        } catch (e: JwtException) {
            throw CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR)
        }
    }

    companion object {
        private const val JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"
        private val ISSUERS = setOf("accounts.google.com", "https://accounts.google.com")
    }
}

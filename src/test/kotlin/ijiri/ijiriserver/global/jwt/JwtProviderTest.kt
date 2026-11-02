package ijiri.ijiriserver.global.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtProviderTest {

    private val jwtProvider = JwtProvider("test-secret-key-0123456789abcdef-0123456789", 3600, 60)

    @Test
    fun access_token_은_한_번_파싱해서_세션과_회원을_꺼낸다() {
        val accessToken = jwtProvider.createAccessToken("7", "USER", "session-1")

        val claims = jwtProvider.parseAccessToken(accessToken)

        assertThat(claims).isNotNull
        assertThat(jwtProvider.getSessionId(claims!!)).isEqualTo("session-1")
        assertThat(jwtProvider.getAuthentication(claims).principal).isEqualTo("7")
    }

    @Test
    fun refresh_token_이나_위조된_토큰은_access_token_으로_받지_않는다() {
        assertThat(jwtProvider.parseAccessToken(jwtProvider.createRefreshToken("7", "session-1"))).isNull()
        assertThat(jwtProvider.parseAccessToken("not-a-jwt")).isNull()
    }
}

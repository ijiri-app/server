package ijiri.ijiriserver.global.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider("test-secret-key-0123456789abcdef-0123456789", 3600, 60);

    @Test
    void access_token_은_한_번_파싱해서_세션과_회원을_꺼낸다() {
        String accessToken = jwtProvider.createAccessToken("7", "USER", "session-1");

        assertThat(jwtProvider.parseAccessToken(accessToken))
                .hasValueSatisfying(claims -> {
                    assertThat(jwtProvider.getSessionId(claims)).isEqualTo("session-1");
                    assertThat(jwtProvider.getAuthentication(claims).getPrincipal()).isEqualTo("7");
                });
    }

    @Test
    void refresh_token_이나_위조된_토큰은_access_token_으로_받지_않는다() {
        assertThat(jwtProvider.parseAccessToken(jwtProvider.createRefreshToken("7", "session-1"))).isEmpty();
        assertThat(jwtProvider.parseAccessToken("not-a-jwt")).isEmpty();
    }
}

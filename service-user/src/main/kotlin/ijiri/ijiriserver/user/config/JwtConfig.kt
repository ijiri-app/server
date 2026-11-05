package ijiri.ijiriserver.user.config

import ijiri.ijiriserver.common.jwt.JwtCookieManager
import ijiri.ijiriserver.common.jwt.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 토큰은 이 서비스만 발급·검증한다. 다른 서비스는 게이트웨이가 넘긴 X-Member-Id 만 본다.
 */
@Configuration
class JwtConfig(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-validity-seconds}") private val accessTokenValiditySeconds: Long,
    @Value("\${jwt.refresh-token-validity-seconds}") private val refreshTokenValiditySeconds: Long,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>,
) {

    @Bean
    fun jwtProvider(clock: Clock): JwtProvider =
        JwtProvider(secret, accessTokenValiditySeconds, refreshTokenValiditySeconds, clock)

    @Bean
    fun jwtCookieManager(jwtProvider: JwtProvider): JwtCookieManager = JwtCookieManager(jwtProvider, allowedOrigins)
}

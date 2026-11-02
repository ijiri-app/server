package ijiri.ijiriserver.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-validity-seconds}") accessTokenValiditySeconds: Long,
    @Value("\${jwt.refresh-token-validity-seconds}") refreshTokenValiditySeconds: Long,
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val accessTokenValidityMillis: Long = accessTokenValiditySeconds * 1000
    private val refreshTokenValidityMillis: Long = refreshTokenValiditySeconds * 1000

    fun createAccessToken(subject: String, role: String, sessionId: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(subject)
            .claim(ROLE_CLAIM, role)
            .claim(SESSION_CLAIM, sessionId)
            .claim(TYPE_CLAIM, ACCESS_TYPE)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenValidityMillis))
            .signWith(key)
            .compact()
    }

    fun createRefreshToken(subject: String, sessionId: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(subject)
            .id(sessionId)
            .claim(TYPE_CLAIM, REFRESH_TYPE)
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenValidityMillis))
            .signWith(key)
            .compact()
    }

    fun getAccessTokenValiditySeconds(): Long = accessTokenValidityMillis / 1000

    fun getRefreshTokenValiditySeconds(): Long = refreshTokenValidityMillis / 1000

    // 서명·만료·토큰 종류를 한 번의 파싱으로 검증하고, 이후 필요한 값은 이 claims 에서 꺼낸다
    fun parseAccessToken(token: String): Claims? = parseWithType(token, ACCESS_TYPE)

    fun parseRefreshToken(token: String): Claims? = parseWithType(token, REFRESH_TYPE)

    private fun parseWithType(token: String, type: String): Claims? =
        try {
            parse(token).takeIf { type == it.get(TYPE_CLAIM, String::class.java) }
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    fun getSessionId(accessClaims: Claims): String? = accessClaims.get(SESSION_CLAIM, String::class.java)

    fun getAuthentication(claims: Claims): Authentication {
        val role = claims.get(ROLE_CLAIM, String::class.java)
        return UsernamePasswordAuthenticationToken(
            claims.subject,
            null,
            listOf(SimpleGrantedAuthority(ROLE_PREFIX + role)),
        )
    }

    private fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    companion object {
        private const val ROLE_CLAIM = "role"

        // Spring Security 의 hasRole() 은 "ROLE_" 접두사가 붙은 authority 를 기대한다
        private const val ROLE_PREFIX = "ROLE_"
        private const val TYPE_CLAIM = "type"

        // access token 이 속한 로그인 세션. refresh token 의 jti 와 같은 값
        private const val SESSION_CLAIM = "sid"
        private const val ACCESS_TYPE = "access"
        private const val REFRESH_TYPE = "refresh"
    }
}

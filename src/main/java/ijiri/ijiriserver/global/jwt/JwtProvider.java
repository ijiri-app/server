package ijiri.ijiriserver.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    // Spring Security 의 hasRole() 은 "ROLE_" 접두사가 붙은 authority 를 기대한다
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TYPE_CLAIM = "type";
    // access token 이 속한 로그인 세션. refresh token 의 jti 와 같은 값
    private static final String SESSION_CLAIM = "sid";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;
    private final long accessTokenValidityMillis;
    private final long refreshTokenValidityMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMillis = accessTokenValiditySeconds * 1000;
        this.refreshTokenValidityMillis = refreshTokenValiditySeconds * 1000;
    }

    public String createAccessToken(String subject, String role, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role)
                .claim(SESSION_CLAIM, sessionId)
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenValidityMillis))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .id(sessionId)
                .claim(TYPE_CLAIM, REFRESH_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenValidityMillis))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMillis / 1000;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValidityMillis / 1000;
    }

    public boolean validateRefreshToken(String token) {
        return hasType(token, REFRESH_TYPE);
    }

    public String getSubject(String token) {
        return parse(token).getSubject();
    }

    // 서명·만료·토큰 종류를 한 번의 파싱으로 검증하고, 이후 필요한 값은 이 claims 에서 꺼낸다
    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = parse(token);
            return ACCESS_TYPE.equals(claims.get(TYPE_CLAIM, String.class)) ? Optional.of(claims) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String getSessionId(Claims accessClaims) {
        return accessClaims.get(SESSION_CLAIM, String.class);
    }

    private boolean hasType(String token, String type) {
        try {
            return type.equals(parse(token).get(TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);
        return new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role))
        );
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

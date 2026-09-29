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
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    // Spring Security 의 hasRole() 은 "ROLE_" 접두사가 붙은 authority 를 기대한다
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;
    private final long accessTokenValidityMillis;
    private final long refreshTokenValidityMillis;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
                       @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMillis = accessTokenValiditySeconds * 1000;
        this.refreshTokenValidityMillis = refreshTokenValiditySeconds * 1000;
    }

    public String createAccessToken(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role)
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenValidityMillis))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
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

    public boolean validateAccessToken(String token) {
        return hasType(token, ACCESS_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return hasType(token, REFRESH_TYPE);
    }

    public String getSubject(String token) {
        return parse(token).getSubject();
    }

    private boolean hasType(String token, String type) {
        try {
            return type.equals(parse(token).get(TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parse(token);
        String role = claims.get(ROLE_CLAIM, String.class);
        return new UsernamePasswordAuthenticationToken(
                claims.getSubject(), null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role))
        );
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

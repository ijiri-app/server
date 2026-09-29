package ijiri.ijiriserver.global.oauth2.apple;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Apple 은 고정 client secret 이 없고, .p8 키로 서명한 ES256 JWT 를 client_secret 으로 쓴다.
 */
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final Duration VALIDITY = Duration.ofMinutes(5);

    private final String teamId;
    private final String keyId;
    private final String privateKeyPem;
    private PrivateKey privateKey;

    public AppleClientSecretGenerator(@Value("${apple.team-id}") String teamId,
                                      @Value("${apple.key-id}") String keyId,
                                      @Value("${apple.private-key}") String privateKey) {
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKeyPem = privateKey;
    }

    public String generate(String clientId) {
        // 키 미설정 상태에서도 서버가 뜨도록 실제 Apple 로그인 시점에 파싱
        if (privateKey == null) {
            privateKey = parsePrivateKey(privateKeyPem);
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(APPLE_AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(VALIDITY)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    private static PrivateKey parsePrivateKey(String pem) {
        try {
            String base64 = pem
                    .replace("\\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid APPLE_PRIVATE_KEY", e);
        }
    }
}

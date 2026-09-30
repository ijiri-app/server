package ijiri.ijiriserver.domain.auth.google.client;

import ijiri.ijiriserver.domain.auth.common.client.SocialTokenVerifier;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

/**
 * Google ID token 을 Google 공개키(JWKS)로 서명 검증하고 iss, aud, exp 를 확인한다.
 */
@Component
public class GoogleOAuthClient implements SocialTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder jwtDecoder;

    public GoogleOAuthClient(
            @Value("${oauth.google.client-ids}") List<String> clientIds,
            ClientHttpRequestFactory externalApiRequestFactory
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
                .restOperations(new RestTemplate(externalApiRequestFactory))
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<String>(JwtClaimNames.ISS, ISSUERS::contains),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        aud -> aud != null && aud.stream().anyMatch(clientIds::contains)
                )
        ));
        this.jwtDecoder = decoder;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public MemberRegisterCommand verify(String idToken) {
        Jwt jwt = decode(idToken);
        // 구글이 소유를 확인하지 않은 이메일은 믿을 수 없으므로 저장하지 않는다
        String email = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
                ? jwt.getClaimAsString("email")
                : null;
        return new MemberRegisterCommand(
                Provider.GOOGLE,
                jwt.getSubject(),
                email,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture")
        );
    }

    // 토큰 자체가 잘못된 경우와 JWKS 조회 실패(구글 서버 문제)를 구분한다
    private Jwt decode(String idToken) {
        try {
            return jwtDecoder.decode(idToken);
        } catch (BadJwtException e) {
            throw new CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR);
        }
    }
}

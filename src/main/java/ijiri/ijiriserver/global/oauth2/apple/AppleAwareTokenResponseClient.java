package ijiri.ijiriserver.global.oauth2.apple;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

/**
 * Apple 토큰 요청 시에만 client_secret 을 매번 새로 생성해서 끼워 넣는다.
 */
@Component
@RequiredArgsConstructor
public class AppleAwareTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final AppleClientSecretGenerator appleClientSecretGenerator;
    private final RestClientAuthorizationCodeTokenResponseClient delegate =
            new RestClientAuthorizationCodeTokenResponseClient();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        ClientRegistration registration = grantRequest.getClientRegistration();
        if (!"apple".equals(registration.getRegistrationId())) {
            return delegate.getTokenResponse(grantRequest);
        }

        ClientRegistration withSecret = ClientRegistration.withClientRegistration(registration)
                .clientSecret(appleClientSecretGenerator.generate(registration.getClientId()))
                .build();
        return delegate.getTokenResponse(
                new OAuth2AuthorizationCodeGrantRequest(withSecret, grantRequest.getAuthorizationExchange()));
    }
}

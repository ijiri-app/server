package ijiri.ijiriserver.domain.auth.kakao.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import ijiri.ijiriserver.domain.auth.common.client.SocialTokenVerifier;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthClient implements SocialTokenVerifier {

    private static final String BASE_URL = "https://kapi.kakao.com";

    private final RestClient restClient;
    private final long appId;

    public KakaoOAuthClient(
            @Value("${oauth.kakao.app-id}") long appId,
            ClientHttpRequestFactory externalApiRequestFactory
    ) {
        this.appId = appId;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(externalApiRequestFactory)
                .build();
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public MemberRegisterCommand verify(String accessToken) {
        // 다른 앱에서 발급된 카카오 토큰으로 로그인하는 걸 막기 위해 app_id 확인
        TokenInfo tokenInfo = get("/v1/user/access_token_info", accessToken, TokenInfo.class);
        if (tokenInfo.appId() != appId) {
            throw new CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN);
        }

        KakaoUser user = get("/v2/user/me", accessToken, KakaoUser.class);
        KakaoAccount account = user.kakaoAccount();
        Profile profile = account != null ? account.profile() : null;
        return new MemberRegisterCommand(
                Provider.KAKAO,
                String.valueOf(user.id()),
                resolveEmail(account),
                profile != null ? profile.nickname() : null,
                profile != null && !profile.isDefaultImage() ? profile.profileImageUrl() : null
        );
    }

    // 카카오가 인증을 확인한 이메일만 사용한다 (미동의 또는 미인증이면 null)
    private String resolveEmail(KakaoAccount account) {
        if (account == null || !account.isEmailValid() || !account.isEmailVerified()) {
            return null;
        }
        return account.email();
    }

    private <T> T get(String uri, String accessToken, Class<T> type) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), (req, res) -> {
                        throw new CustomException(AuthStatusCode.INVALID_PROVIDER_TOKEN);
                    })
                    .body(type);
        } catch (RestClientException e) {
            throw new CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR);
        }
    }

    private record TokenInfo(@JsonProperty("app_id") long appId) {
    }

    private record KakaoUser(long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    private record KakaoAccount(
            String email,
            @JsonProperty("is_email_valid") boolean isEmailValid,
            @JsonProperty("is_email_verified") boolean isEmailVerified,
            Profile profile
    ) {
    }

    private record Profile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl,
            @JsonProperty("is_default_image") boolean isDefaultImage
    ) {
    }
}

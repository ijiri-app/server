package ijiri.ijiriserver.domain.auth.kakao.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import ijiri.ijiriserver.domain.auth.common.dto.SocialUserInfo;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthClient {

    private final RestClient restClient = RestClient.create("https://kapi.kakao.com");
    private final long appId;

    public KakaoOAuthClient(@Value("${oauth.kakao.app-id}") long appId) {
        this.appId = appId;
    }

    public SocialUserInfo getUserInfo(String accessToken) {
        // 다른 앱에서 발급된 카카오 토큰으로 로그인하는 걸 막기 위해 app_id 확인
        TokenInfo tokenInfo = get("/v1/user/access_token_info", accessToken, TokenInfo.class);
        if (tokenInfo.appId() != appId) {
            throw new CustomException(AuthStatusCode.INVALID_SOCIAL_TOKEN);
        }

        KakaoUser user = get("/v2/user/me", accessToken, KakaoUser.class);
        KakaoAccount account = user.kakaoAccount();
        return new SocialUserInfo(
                Provider.KAKAO,
                String.valueOf(user.id()),
                account != null ? account.email() : null,
                account != null && account.profile() != null ? account.profile().nickname() : null);
    }

    private <T> T get(String uri, String accessToken, Class<T> type) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), (req, res) -> {
                        throw new CustomException(AuthStatusCode.INVALID_SOCIAL_TOKEN);
                    })
                    .body(type);
        } catch (RestClientException e) {
            throw new CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR);
        }
    }

    private record TokenInfo(long id, @JsonProperty("app_id") long appId) {
    }

    private record KakaoUser(long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    private record KakaoAccount(String email, Profile profile) {
    }

    private record Profile(String nickname) {
    }
}

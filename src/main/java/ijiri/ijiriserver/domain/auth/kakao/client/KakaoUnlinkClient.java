package ijiri.ijiriserver.domain.auth.kakao.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import ijiri.ijiriserver.domain.auth.common.client.SocialUnlinkClient;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 어드민 키로 카카오 연결 끊기. 사용자 토큰 없이 서버가 직접 호출한다.
 */
@Slf4j
@Component
public class KakaoUnlinkClient implements SocialUnlinkClient {

    // 이미 연결이 끊긴 사용자 (카카오 계정 설정에서 직접 끊은 경우 등)
    private static final int NOT_REGISTERED_USER = -101;

    private final RestClient restClient = RestClient.create("https://kapi.kakao.com");
    private final String adminKey;

    public KakaoUnlinkClient(@Value("${oauth.kakao.admin-key}") String adminKey) {
        this.adminKey = adminKey;
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public void unlink(String providerMemberId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerMemberId);

        try {
            restClient.post()
                    .uri("/v1/user/unlink")
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            KakaoError error = e.getResponseBodyAs(KakaoError.class);
            if (error != null && error.code() == NOT_REGISTERED_USER) {
                return;
            }
            log.error("Kakao unlink failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR);
        } catch (RestClientException e) {
            log.error("Kakao unlink failed", e);
            throw new CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR);
        }
    }

    private record KakaoError(@JsonProperty("code") int code) {
    }
}

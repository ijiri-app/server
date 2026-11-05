package ijiri.ijiriserver.user.auth.common.adapter.out.external.kakao

import com.fasterxml.jackson.annotation.JsonProperty
import ijiri.ijiriserver.user.auth.common.application.port.out.SocialUnlinkClient
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.common.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * 어드민 키로 카카오 연결 끊기. 사용자 토큰 없이 서버가 직접 호출한다.
 */
@Component
class KakaoUnlinkClient(
    @Value("\${oauth.kakao.admin-key}") private val adminKey: String,
    externalApiRequestFactory: ClientHttpRequestFactory,
) : SocialUnlinkClient {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .requestFactory(externalApiRequestFactory)
        .build()

    override fun provider(): Provider = Provider.KAKAO

    override fun unlink(providerMemberId: String) {
        val form = LinkedMultiValueMap<String, String>()
        form.add("target_id_type", "user_id")
        form.add("target_id", providerMemberId)

        try {
            restClient.post()
                .uri("/v1/user/unlink")
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK $adminKey")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity()
        } catch (e: HttpClientErrorException) {
            val error = e.getResponseBodyAs(KakaoError::class.java)
            if (error != null && error.code == NOT_REGISTERED_USER) {
                return
            }
            log.error("Kakao unlink failed: status={}, body={}", e.statusCode, e.responseBodyAsString)
            throw CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR)
        } catch (e: RestClientException) {
            log.error("Kakao unlink failed", e)
            throw CustomException(AuthStatusCode.SOCIAL_SERVER_ERROR)
        }
    }

    private data class KakaoError(@JsonProperty("code") val code: Int = 0)

    companion object {
        // 이미 연결이 끊긴 사용자 (카카오 계정 설정에서 직접 끊은 경우 등)
        private const val NOT_REGISTERED_USER = -101

        private const val BASE_URL = "https://kapi.kakao.com"

        private val log = LoggerFactory.getLogger(KakaoUnlinkClient::class.java)
    }
}

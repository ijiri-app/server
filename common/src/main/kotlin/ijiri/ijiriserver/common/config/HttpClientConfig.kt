package ijiri.ijiriserver.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Duration

/**
 * 외부 API(카카오, 구글 JWKS)와 내부 서비스 호출용. 상대가 멈춰도 요청 스레드가 무한정 묶이지 않도록 타임아웃을 건다.
 */
@Configuration
class HttpClientConfig {

    @Bean
    fun externalApiRequestFactory(): ClientHttpRequestFactory = factory(EXTERNAL_CONNECT_TIMEOUT, EXTERNAL_READ_TIMEOUT)

    // 같은 compose 네트워크 안이라 연결은 빨라야 하고, 부품 등록처럼 DB 를 거치는 호출은 조금 길게 기다린다
    @Bean
    fun internalApiRequestFactory(): ClientHttpRequestFactory = factory(INTERNAL_CONNECT_TIMEOUT, INTERNAL_READ_TIMEOUT)

    private fun factory(connectTimeout: Duration, readTimeout: Duration): ClientHttpRequestFactory {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(connectTimeout)
        factory.setReadTimeout(readTimeout)
        return factory
    }

    companion object {
        private val EXTERNAL_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
        private val EXTERNAL_READ_TIMEOUT: Duration = Duration.ofSeconds(5)
        private val INTERNAL_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        private val INTERNAL_READ_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}

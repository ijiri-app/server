package ijiri.ijiriserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Duration

/**
 * 외부 API(카카오, 구글 JWKS) 호출용. 상대 서버가 멈춰도 요청 스레드가 무한정 묶이지 않도록 타임아웃을 건다.
 */
@Configuration
class HttpClientConfig {

    @Bean
    fun externalApiRequestFactory(): ClientHttpRequestFactory {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(CONNECT_TIMEOUT)
        factory.setReadTimeout(READ_TIMEOUT)
        return factory
    }

    companion object {
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
        private val READ_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}

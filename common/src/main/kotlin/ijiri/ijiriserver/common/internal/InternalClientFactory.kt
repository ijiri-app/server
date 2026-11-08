package ijiri.ijiriserver.common.internal

import ijiri.ijiriserver.common.exception.CommonStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 서비스 주소(services.<name>.url)마다 InternalClient 를 만든다. 호출하는 어댑터가 자기 주소로 하나씩 만든다.
 * 서킷은 주소별로 하나이며, 연결 실패와 5xx(SERVICE_UNAVAILABLE)만 실패로 센다. 4xx 는 상대가 정상적으로 답한 것이다.
 */
@Component
class InternalClientFactory(
    @Qualifier("internalApiRequestFactory") private val requestFactory: ClientHttpRequestFactory,
) {

    private val circuitBreakers = CircuitBreakerRegistry.of(
        CircuitBreakerConfig.custom()
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .waitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE)
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN_STATE)
            .recordException { it is CustomException && it.statusCode == CommonStatusCode.SERVICE_UNAVAILABLE }
            .build(),
    )

    fun forService(baseUrl: String): InternalClient {
        val url = baseUrl.trimEnd('/')
        return InternalClient(
            RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build(),
            circuitBreakers.circuitBreaker(url),
        )
    }

    companion object {
        // 최근 20번 중 10번 이상 부른 뒤 실패가 절반을 넘으면 열고, 10초 뒤 3번 시험해 닫을지 정한다
        private const val SLIDING_WINDOW_SIZE = 20
        private const val MINIMUM_NUMBER_OF_CALLS = 10
        private const val FAILURE_RATE_THRESHOLD = 50f
        private val WAIT_DURATION_IN_OPEN_STATE: Duration = Duration.ofSeconds(10)
        private const val PERMITTED_CALLS_IN_HALF_OPEN_STATE = 3
    }
}

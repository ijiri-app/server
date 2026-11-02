package ijiri.ijiriserver.common.internal

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 서비스 주소(services.<name>.url)마다 InternalClient 를 만든다. 호출하는 어댑터가 자기 주소로 하나씩 만든다.
 */
@Component
class InternalClientFactory(
    @Qualifier("internalApiRequestFactory") private val requestFactory: ClientHttpRequestFactory,
) {

    fun forService(baseUrl: String): InternalClient =
        InternalClient(
            RestClient.builder()
                .baseUrl(baseUrl.trimEnd('/'))
                .requestFactory(requestFactory)
                .build(),
        )
}

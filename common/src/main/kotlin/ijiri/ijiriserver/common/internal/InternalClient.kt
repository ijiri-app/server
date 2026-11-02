package ijiri.ijiriserver.common.internal

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ijiri.ijiriserver.common.exception.CommonStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.exception.RemoteStatusCode
import ijiri.ijiriserver.common.response.BaseResponse
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * 다른 서비스의 /internal API 호출. 응답 본문은 BaseResponse 이며, 오류 응답은 그 code·message 를 담은
 * CustomException 으로 바꿔 던지고(RemoteStatusCode), 연결 실패나 깨진 응답은 SERVICE_UNAVAILABLE 로 답한다.
 */
class InternalClient(
    @PublishedApi internal val restClient: RestClient,
) {

    inline fun <reified T : Any> get(uri: String, vararg uriVariables: Any): T =
        exchange(restClient.get().uri(uri, *uriVariables), object : ParameterizedTypeReference<BaseResponse<T>>() {})

    inline fun <reified T : Any> post(uri: String, body: Any, vararg uriVariables: Any): T =
        exchange(
            restClient.post().uri(uri, *uriVariables).body(body),
            object : ParameterizedTypeReference<BaseResponse<T>>() {},
        )

    // 본문이 없는 204 응답
    fun postWithoutResult(uri: String, body: Any, vararg uriVariables: Any) {
        call {
            restClient.post()
                .uri(uri, *uriVariables)
                .body(body)
                .retrieve()
                .onStatus(ERROR, ::fail)
                .toBodilessEntity()
        }
    }

    @PublishedApi
    internal fun <T : Any> exchange(
        spec: RestClient.RequestHeadersSpec<*>,
        type: ParameterizedTypeReference<BaseResponse<T>>,
    ): T =
        call { spec.retrieve().onStatus(ERROR, ::fail).body(type)?.result }
            ?: throw unavailable("empty body")

    private fun <T> call(action: () -> T): T {
        try {
            return action()
        } catch (e: RestClientException) {
            throw unavailable(e.message)
        }
    }

    // 상대 서비스의 BaseResponse 오류를 그대로 옮긴다 (4xx 는 그 code, 5xx 는 SERVICE_UNAVAILABLE)
    private fun fail(request: HttpRequest, response: ClientHttpResponse) {
        val status = HttpStatus.resolve(response.statusCode.value())
        val body = runCatching { OBJECT_MAPPER.readValue(response.body, ERROR_BODY) }.getOrNull()
        if (status == null || status.is5xxServerError || body == null) {
            throw unavailable("status=${response.statusCode}")
        }
        throw CustomException(RemoteStatusCode(status, body.code, body.message), body.result)
    }

    private fun unavailable(detail: String?): CustomException {
        log.error("Internal API call failed: {}", detail)
        return CustomException(CommonStatusCode.SERVICE_UNAVAILABLE)
    }

    companion object {
        private val ERROR = { status: HttpStatusCode -> status.isError }
        private val OBJECT_MAPPER = jacksonObjectMapper().findAndRegisterModules()
        private val ERROR_BODY = object : TypeReference<BaseResponse<Map<String, Any?>>>() {}
        private val log = LoggerFactory.getLogger(InternalClient::class.java)
    }
}

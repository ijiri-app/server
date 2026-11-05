package ijiri.ijiriserver.common.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.common.exception.CommonStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 메시지는 JSON 문자열로 보낸다. 타입 헤더 없이 토픽마다 메시지 클래스가 정해져 있어 소비자가 그 클래스로 읽는다.
 */
@Component
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : MessagePublisher {

    override fun publish(topic: String, key: String, message: Any) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(message))
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            throw publishFailed(topic, key, e)
        } catch (e: TimeoutException) {
            throw publishFailed(topic, key, e)
        }
    }

    private fun publishFailed(topic: String, key: String, e: Exception): CustomException {
        log.error("Kafka publish failed: topic={}, key={}", topic, key, e)
        return CustomException(CommonStatusCode.SERVICE_UNAVAILABLE)
    }

    companion object {
        private const val SEND_TIMEOUT_SECONDS = 5L
        private val log = LoggerFactory.getLogger(KafkaMessagePublisher::class.java)
    }
}

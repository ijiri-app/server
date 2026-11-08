package ijiri.ijiriserver.common.messaging

import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries

/**
 * 리스너가 실패하면 간격을 늘려 가며 다시 시도하고, 그래도 실패하면 `<토픽>.<그룹>.DLT` 로 옮긴다 (로그만 남기고
 * 버리지 않는다). 같은 토픽을 여러 서비스가 소비하므로 DLT 는 그룹별로 나눈다. JSON 이 깨진 메시지는 다시 시도해도
 * 소용없으므로 바로 옮긴다. DLT 토픽은 브로커가 처음 쓸 때 만든다 (auto.create.topics.enable 기본값).
 */
@Configuration
class KafkaErrorHandlerConfig {

    @Bean
    fun kafkaErrorHandler(
        kafkaTemplate: KafkaTemplate<String, String>,
        @Value("\${spring.kafka.consumer.group-id}") groupId: String,
    ): DefaultErrorHandler {
        // 파티션을 비워(-1) 브로커가 고르게 한다. DLT 파티션 수가 원래 토픽과 달라도 된다
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record: ConsumerRecord<*, *>, _: Exception ->
            TopicPartition("${record.topic()}.$groupId$DLT_SUFFIX", -1)
        }
        val backOff = ExponentialBackOffWithMaxRetries(MAX_RETRIES).apply {
            initialInterval = INITIAL_INTERVAL_MILLIS
            multiplier = MULTIPLIER
            maxInterval = MAX_INTERVAL_MILLIS
        }
        return DefaultErrorHandler(recoverer, backOff).apply {
            addNotRetryableExceptions(JsonProcessingException::class.java)
        }
    }

    companion object {
        const val DLT_SUFFIX = ".DLT"

        // 1, 2, 4, 8, 16, 30초: 약 1분 동안 DB·다른 서비스의 짧은 장애를 넘긴다
        private const val MAX_RETRIES = 6
        private const val INITIAL_INTERVAL_MILLIS = 1000L
        private const val MULTIPLIER = 2.0
        private const val MAX_INTERVAL_MILLIS = 30_000L
    }
}

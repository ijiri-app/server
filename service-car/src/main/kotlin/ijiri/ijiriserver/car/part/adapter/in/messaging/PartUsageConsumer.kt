package ijiri.ijiriserver.car.part.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.car.part.application.port.`in`.RecordPartUsageUseCase
import ijiri.ijiriserver.common.messaging.MessageHeader
import ijiri.ijiriserver.common.messaging.PartUsageChangedMessage
import ijiri.ijiriserver.common.messaging.Topics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * 게시물의 부품이 바뀌면 전체·차종별 사용 수를 맞춘다. at-least-once 로 같은 메시지가 다시 올 수 있어
 * message-id 헤더로 이미 센 메시지를 건너뛴다.
 */
@Component
class PartUsageConsumer(
    private val recordPartUsageUseCase: RecordPartUsageUseCase,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = [Topics.PART_USAGE_CHANGED])
    fun onPartUsageChanged(
        @Payload payload: String,
        @Header(name = MessageHeader.ID, required = false) messageId: ByteArray?,
    ) {
        val message = objectMapper.readValue(payload, PartUsageChangedMessage::class.java)
        recordPartUsageUseCase.recordUsage(
            messageId?.let { String(it, Charsets.UTF_8) },
            message.carModelId,
            message.addedPartIds,
            message.removedPartIds,
        )
    }
}

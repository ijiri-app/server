package ijiri.ijiriserver.car.part.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.car.part.application.port.`in`.RecordPartUsageUseCase
import ijiri.ijiriserver.common.messaging.PartUsageChangedMessage
import ijiri.ijiriserver.common.messaging.Topics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 게시물의 부품이 바뀌면 전체·차종별 사용 수를 맞춘다. 같은 메시지를 두 번 받으면 두 번 세므로(at-least-once)
 * 순위용 수치로만 쓴다.
 */
@Component
class PartUsageConsumer(
    private val recordPartUsageUseCase: RecordPartUsageUseCase,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = [Topics.PART_USAGE_CHANGED])
    fun onPartUsageChanged(payload: String) {
        val message = objectMapper.readValue(payload, PartUsageChangedMessage::class.java)
        recordPartUsageUseCase.recordUsage(message.addedPartIds, message.carModelId, 1)
        recordPartUsageUseCase.recordUsage(message.removedPartIds, message.carModelId, -1)
    }
}

package ijiri.ijiriserver.car.part.application.port.out

import java.time.LocalDateTime

interface ProcessedMessagePort {

    /**
     * 처음 보는 메시지면 기록하고 true, 이미 처리한 메시지면 false.
     */
    fun markProcessed(messageId: String, processedAt: LocalDateTime): Boolean
}

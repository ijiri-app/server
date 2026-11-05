package ijiri.ijiriserver.car.part.adapter.out.persistence

import ijiri.ijiriserver.car.part.application.port.out.ProcessedMessagePort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * 중복 처리 방지용 기록이라 엔티티 없이 INSERT 한 번으로 처리 여부를 정한다.
 */
@Component
class ProcessedMessagePersistenceAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : ProcessedMessagePort {

    override fun markProcessed(messageId: String, processedAt: LocalDateTime): Boolean =
        jdbcTemplate.update(
            "INSERT INTO processed_message (message_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING",
            messageId,
            Timestamp.valueOf(processedAt),
        ) == 1
}

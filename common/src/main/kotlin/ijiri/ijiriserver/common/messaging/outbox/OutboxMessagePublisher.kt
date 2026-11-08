package ijiri.ijiriserver.common.messaging.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.common.messaging.MessagePublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Transactional outbox. 메시지를 호출한 트랜잭션 안에서 outbox_message 에 쓰므로, 업무 데이터와 메시지가 함께
 * 커밋되거나 함께 롤백된다. 발행하는 서비스만 messaging.outbox.enabled=true 로 켜고 테이블을 마이그레이션한다.
 */
@Component
@ConditionalOnProperty(prefix = "messaging.outbox", name = ["enabled"], havingValue = "true")
class OutboxMessagePublisher(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : MessagePublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    override fun publish(topic: String, key: String, message: Any) {
        jdbcTemplate.update(
            "INSERT INTO outbox_message (message_id, topic, message_key, payload, created_at) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID().toString(),
            topic,
            key,
            objectMapper.writeValueAsString(message),
            Timestamp.valueOf(LocalDateTime.now(clock)),
        )
    }
}

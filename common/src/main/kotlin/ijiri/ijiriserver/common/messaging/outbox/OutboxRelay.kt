package ijiri.ijiriserver.common.messaging.outbox

import ijiri.ijiriserver.common.messaging.MessageHeader
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 커밋된 outbox 메시지를 id 순서로 브로커에 보내고, 브로커가 받은 것만 지운다. 보내다 실패하면 거기서 멈춰
 * 다음 주기에 그 메시지부터 다시 보내므로 순서가 지켜지고, 중복은 생길 수 있다(at-least-once).
 * blue/green 이 겹치는 동안에는 두 인스턴스가 함께 돌지만 SKIP LOCKED 로 같은 행을 나눠 갖지 않는다.
 */
@Component
@ConditionalOnProperty(prefix = "messaging.outbox", name = ["enabled"], havingValue = "true")
class OutboxRelay(
    private val jdbcTemplate: JdbcTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val transactionTemplate: TransactionTemplate,
) {

    @Scheduled(fixedDelay = RELAY_INTERVAL_MILLIS)
    fun relay() {
        do {
            val sent = transactionTemplate.execute { relayBatch() } ?: 0
        } while (sent == BATCH_SIZE)
    }

    private fun relayBatch(): Int {
        val messages = jdbcTemplate.query(
            """
            SELECT id, message_id, topic, message_key, payload FROM outbox_message
            ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED
            """.trimIndent(),
            { rs, _ ->
                OutboxMessage(
                    rs.getLong("id"),
                    rs.getString("message_id"),
                    rs.getString("topic"),
                    rs.getString("message_key"),
                    rs.getString("payload"),
                )
            },
            BATCH_SIZE,
        )
        val sentIds = messages.asSequence().takeWhile(::send).map(OutboxMessage::id).toList()
        jdbcTemplate.batchUpdate("DELETE FROM outbox_message WHERE id = ?", sentIds.map { arrayOf<Any>(it) })
        return sentIds.size
    }

    private fun send(message: OutboxMessage): Boolean {
        val record = ProducerRecord<String, String>(message.topic, message.key, message.payload)
        record.headers().add(MessageHeader.ID, message.messageId.toByteArray())
        return try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            true
        } catch (e: ExecutionException) {
            sendFailed(message, e)
        } catch (e: TimeoutException) {
            sendFailed(message, e)
        }
    }

    private fun sendFailed(message: OutboxMessage, e: Exception): Boolean {
        log.error("Outbox relay failed: topic={}, key={}", message.topic, message.key, e)
        return false
    }

    private data class OutboxMessage(
        val id: Long,
        val messageId: String,
        val topic: String,
        val key: String,
        val payload: String,
    )

    companion object {
        private const val RELAY_INTERVAL_MILLIS = 1000L
        private const val BATCH_SIZE = 100
        private const val SEND_TIMEOUT_SECONDS = 5L
        private val log = LoggerFactory.getLogger(OutboxRelay::class.java)
    }
}

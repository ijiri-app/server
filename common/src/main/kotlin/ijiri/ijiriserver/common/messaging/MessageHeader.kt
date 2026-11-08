package ijiri.ijiriserver.common.messaging

/**
 * 모든 메시지에 붙는 Kafka 헤더. 중복 처리를 막아야 하는 소비자는 ID 로 이미 처리한 메시지를 건너뛴다.
 */
object MessageHeader {
    const val ID = "message-id"
}

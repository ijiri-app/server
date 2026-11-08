package ijiri.ijiriserver.common.messaging

/**
 * 이벤트 발행. 호출한 트랜잭션 안에서 outbox 에 기록되고, 커밋된 뒤에만 브로커로 나간다 (OutboxRelay).
 * 같은 키의 메시지는 같은 파티션에 들어가 순서가 지켜진다.
 */
interface MessagePublisher {

    fun publish(topic: String, key: String, message: Any)
}

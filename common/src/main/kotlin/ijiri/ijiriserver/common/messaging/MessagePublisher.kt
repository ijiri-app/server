package ijiri.ijiriserver.common.messaging

/**
 * 이벤트 발행. 같은 키의 메시지는 같은 파티션에 들어가 순서가 지켜진다.
 * 브로커가 메시지를 받았다고 답할 때까지 기다리므로, 트랜잭션 안에서 부르면 발행 실패가 롤백으로 이어진다.
 */
interface MessagePublisher {

    fun publish(topic: String, key: String, message: Any)
}

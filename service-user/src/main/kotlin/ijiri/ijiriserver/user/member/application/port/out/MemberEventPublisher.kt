package ijiri.ijiriserver.user.member.application.port.out

/**
 * 다른 서비스에 알리는 회원 이벤트. 브로커가 받았다고 답할 때까지 기다리므로 트랜잭션 안에서 부르면 실패가 롤백으로 이어진다.
 */
interface MemberEventPublisher {

    fun withdrawn(memberId: Long)

    fun purged(memberId: Long)
}

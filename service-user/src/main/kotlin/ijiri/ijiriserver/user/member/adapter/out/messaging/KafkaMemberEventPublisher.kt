package ijiri.ijiriserver.user.member.adapter.out.messaging

import ijiri.ijiriserver.common.messaging.MemberPurgedMessage
import ijiri.ijiriserver.common.messaging.MemberWithdrawnMessage
import ijiri.ijiriserver.common.messaging.MessagePublisher
import ijiri.ijiriserver.common.messaging.Topics
import ijiri.ijiriserver.user.member.application.port.out.MemberEventPublisher
import org.springframework.stereotype.Component

@Component
class KafkaMemberEventPublisher(
    private val messagePublisher: MessagePublisher,
) : MemberEventPublisher {

    override fun withdrawn(memberId: Long) {
        messagePublisher.publish(Topics.MEMBER_WITHDRAWN, memberId.toString(), MemberWithdrawnMessage(memberId))
    }

    override fun purged(memberId: Long) {
        messagePublisher.publish(Topics.MEMBER_PURGED, memberId.toString(), MemberPurgedMessage(memberId))
    }
}

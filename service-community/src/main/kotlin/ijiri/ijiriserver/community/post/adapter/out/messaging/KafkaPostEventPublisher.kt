package ijiri.ijiriserver.community.post.adapter.out.messaging

import ijiri.ijiriserver.common.messaging.MessagePublisher
import ijiri.ijiriserver.common.messaging.PartUsageChangedMessage
import ijiri.ijiriserver.common.messaging.PostDeletedMessage
import ijiri.ijiriserver.common.messaging.Topics
import ijiri.ijiriserver.community.post.application.port.out.PostEventPublisher
import org.springframework.stereotype.Component

@Component
class KafkaPostEventPublisher(
    private val messagePublisher: MessagePublisher,
) : PostEventPublisher {

    override fun postDeleted(postId: Long, imageKeys: List<String>) {
        messagePublisher.publish(Topics.POST_DELETED, postId.toString(), PostDeletedMessage(postId, imageKeys))
    }

    override fun partUsageChanged(carModelId: Long, addedPartIds: Set<Long>, removedPartIds: Set<Long>) {
        if (addedPartIds.isEmpty() && removedPartIds.isEmpty()) {
            return
        }
        messagePublisher.publish(
            Topics.PART_USAGE_CHANGED,
            carModelId.toString(),
            PartUsageChangedMessage(carModelId, addedPartIds, removedPartIds),
        )
    }
}

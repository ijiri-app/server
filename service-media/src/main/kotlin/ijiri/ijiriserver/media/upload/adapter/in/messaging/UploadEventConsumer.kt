package ijiri.ijiriserver.media.upload.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.common.messaging.MemberPurgedMessage
import ijiri.ijiriserver.common.messaging.PostDeletedMessage
import ijiri.ijiriserver.common.messaging.Topics
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteMemberImagesUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteUploadedImagesUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 게시물이 지워지면 그 사진을, 회원이 영구 삭제되면 그 회원의 모든 사진을 저장소에서 지운다. 둘 다 멱등하다.
 */
@Component
class UploadEventConsumer(
    private val deleteUploadedImagesUseCase: DeleteUploadedImagesUseCase,
    private val deleteMemberImagesUseCase: DeleteMemberImagesUseCase,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = [Topics.POST_DELETED])
    fun onPostDeleted(payload: String) {
        val message = objectMapper.readValue(payload, PostDeletedMessage::class.java)
        deleteUploadedImagesUseCase.delete(message.imageKeys)
    }

    @KafkaListener(topics = [Topics.MEMBER_PURGED])
    fun onMemberPurged(payload: String) {
        val message = objectMapper.readValue(payload, MemberPurgedMessage::class.java)
        deleteMemberImagesUseCase.deleteAll(message.memberId)
    }
}

package ijiri.ijiriserver.community.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import ijiri.ijiriserver.common.messaging.MemberPurgedMessage
import ijiri.ijiriserver.common.messaging.MemberWithdrawnMessage
import ijiri.ijiriserver.common.messaging.Topics
import ijiri.ijiriserver.community.block.application.port.`in`.DeleteMemberBlocksUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.DeleteMemberPostsUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.HideMemberPostsUseCase
import ijiri.ijiriserver.community.report.application.port.`in`.DeleteMemberReportsUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.DeleteMemberWishesUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.HideMemberWishesUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * service-user 의 회원 이벤트. 네 도메인(게시물, 위시리스트, 차단, 신고)에 걸치므로 서비스 루트의 인바운드 어댑터다.
 * 한 토픽은 서비스당 리스너 하나만 둔다 (같은 그룹의 리스너가 여럿이면 메시지가 나뉘어 간다).
 * 각 단계는 멱등하다. 중간에 실패하면 같은 메시지를 다시 받아 처음부터 반복한다.
 */
@Component
class MemberEventConsumer(
    private val hideMemberPostsUseCase: HideMemberPostsUseCase,
    private val hideMemberWishesUseCase: HideMemberWishesUseCase,
    private val deleteMemberWishesUseCase: DeleteMemberWishesUseCase,
    private val deleteMemberPostsUseCase: DeleteMemberPostsUseCase,
    private val deleteMemberBlocksUseCase: DeleteMemberBlocksUseCase,
    private val deleteMemberReportsUseCase: DeleteMemberReportsUseCase,
    private val objectMapper: ObjectMapper,
) {

    // 탈퇴(soft delete): 게시물과 담기를 숨긴다
    @KafkaListener(topics = [Topics.MEMBER_WITHDRAWN])
    fun onMemberWithdrawn(payload: String) {
        val message = objectMapper.readValue(payload, MemberWithdrawnMessage::class.java)
        hideMemberPostsUseCase.hideAll(message.memberId)
        hideMemberWishesUseCase.hideAll(message.memberId)
    }

    // 영구 삭제: 담기 -> 게시물(부품·태그, 사진 삭제 이벤트) -> 차단 -> 신고
    @KafkaListener(topics = [Topics.MEMBER_PURGED])
    fun onMemberPurged(payload: String) {
        val message = objectMapper.readValue(payload, MemberPurgedMessage::class.java)
        deleteMemberWishesUseCase.deleteAll(message.memberId)
        deleteMemberPostsUseCase.deleteAll(message.memberId)
        deleteMemberBlocksUseCase.deleteAll(message.memberId)
        deleteMemberReportsUseCase.deleteAll(message.memberId)
    }
}

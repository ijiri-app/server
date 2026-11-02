package ijiri.ijiriserver.common.messaging

/**
 * 서비스 사이의 이벤트 토픽. 발행하는 서비스가 NewTopic 빈으로 만든다. 메시지 키는 집계 id(회원 id, 게시물 id)라
 * 같은 대상의 이벤트는 순서대로 소비된다.
 */
object Topics {
    const val MEMBER_WITHDRAWN = "member.withdrawn"
    const val MEMBER_PURGED = "member.purged"
    const val POST_DELETED = "post.deleted"
    const val PART_USAGE_CHANGED = "part.usage-changed"
}

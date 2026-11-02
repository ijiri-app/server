package ijiri.ijiriserver.domain.post.adapter.out.persistence

import ijiri.ijiriserver.domain.member.application.port.out.MemberPostCounter
import ijiri.ijiriserver.domain.ownedcar.application.port.out.OwnedCarPostCounter
import ijiri.ijiriserver.domain.post.application.port.out.PostPort
import ijiri.ijiriserver.domain.post.entity.PostStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 회원·보유 차량 도메인에 게시물 수를 제공한다. 저장소만 의존해 서비스 사이에 순환 의존이 생기지 않게 한다.
 */
@Component
@Transactional(readOnly = true)
class PostCountProvider(
    private val postPort: PostPort,
) : MemberPostCounter, OwnedCarPostCounter {

    override fun countVisiblePosts(memberId: Long): Long =
        postPort.countByMemberIdAndStatus(memberId, PostStatus.PUBLIC)

    override fun countPosts(ownedCarIds: Collection<Long>): Map<Long, Long> {
        if (ownedCarIds.isEmpty()) {
            return emptyMap()
        }
        return postPort.countByOwnedCarIds(ownedCarIds)
            .associate { row -> row[0] as Long to row[1] as Long }
    }
}

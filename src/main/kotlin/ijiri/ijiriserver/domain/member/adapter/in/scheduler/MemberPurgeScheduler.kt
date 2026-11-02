package ijiri.ijiriserver.domain.member.adapter.`in`.scheduler

import ijiri.ijiriserver.domain.member.application.port.`in`.FindMemberPurgeTargetsUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.PurgeMemberUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 탈퇴 후 보관 기간이 지난 회원을 매일 새벽 4시에 완전히 삭제한다. 100명씩 끊어 읽고,
 * 회원마다 별도 트랜잭션으로 지워 한 명의 실패(외부 저장소 오류 등)가 다른 회원 삭제를 막지 않게 한다.
 * 회원 한 명 안에서는 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원 순서로 지운다 (리스너 @Order).
 */
@Component
class MemberPurgeScheduler(
    private val findMemberPurgeTargetsUseCase: FindMemberPurgeTargetsUseCase,
    private val purgeMemberUseCase: PurgeMemberUseCase,
) {

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun purgeWithdrawnMembers() {
        var purged = 0
        var failed = 0
        var afterId = 0L
        var memberIds = findMemberPurgeTargetsUseCase.findPurgeTargetIds(afterId, BATCH_SIZE)
        while (memberIds.isNotEmpty()) {
            for (memberId in memberIds) {
                try {
                    purgeMemberUseCase.purge(memberId)
                    purged++
                } catch (e: RuntimeException) {
                    failed++
                    log.error("Member purge failed: memberId={}", memberId, e)
                }
            }
            afterId = memberIds.last()
            memberIds = findMemberPurgeTargetsUseCase.findPurgeTargetIds(afterId, BATCH_SIZE)
        }
        log.info("Purged {} withdrawn members ({} failed)", purged, failed)
    }

    companion object {
        private const val BATCH_SIZE = 100
        private val log = LoggerFactory.getLogger(MemberPurgeScheduler::class.java)
    }
}

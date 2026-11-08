package ijiri.ijiriserver.user.member.application.service

import ijiri.ijiriserver.user.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.FindMemberPurgeTargetsUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.PurgeMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.SuspendMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.WithdrawMemberUseCase
import ijiri.ijiriserver.user.member.application.port.out.MemberEventPublisher
import ijiri.ijiriserver.user.member.application.port.out.MemberPort
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import ijiri.ijiriserver.user.member.event.MemberSuspendedEvent
import ijiri.ijiriserver.user.member.event.MemberWithdrawnEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MemberLifecycleService(
    private val memberPort: MemberPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val memberEventPublisher: MemberEventPublisher,
    private val clock: Clock,
    private val getMemberUseCase: GetMemberUseCase,
    private val findActiveMemberUseCase: FindActiveMemberUseCase,
) : WithdrawMemberUseCase,
    SuspendMemberUseCase,
    FindMemberPurgeTargetsUseCase,
    PurgeMemberUseCase {

    // soft delete: 행은 보관 기간 동안 남긴다. 이 서비스 안의 즉시 처리(토큰 폐기, 소셜 연결 끊기)는 Spring 이벤트로,
    // 다른 서비스의 처리(게시물·담기 숨김)는 Kafka 로 알린다. 메시지는 outbox 에 함께 커밋되어 탈퇴가 커밋된 뒤에만 나간다
    @Transactional
    override fun withdraw(memberId: Long) {
        val member = getMemberUseCase.getById(memberId)
        member.withdraw(LocalDateTime.now(clock))
        eventPublisher.publishEvent(
            MemberWithdrawnEvent(
                memberId,
                member.provider,
                member.providerMemberId,
            ),
        )
        memberEventPublisher.withdrawn(memberId)
    }

    @Transactional
    override fun suspend(memberId: Long, until: LocalDateTime) {
        findActiveMemberUseCase.findActiveMember(memberId)?.let { member ->
            member.suspend(until)
            eventPublisher.publishEvent(MemberSuspendedEvent(memberId))
        }
    }

    override fun findPurgeTargetIds(afterId: Long, size: Int): List<Long> {
        val cutoff = LocalDateTime.now(clock).minusDays(WITHDRAWAL_RETENTION_DAYS)
        return memberPort.findIdsWithdrawnBefore(cutoff, afterId, Limit.of(size))
    }

    // 이 서비스의 데이터(보유 차량, 관심 차종, 소셜 연결 끊기 재시도)는 Spring 이벤트로 먼저 지우고,
    // 다른 서비스(게시물·담기·차단·신고, 사진 파일)에는 Kafka 로 알린 뒤 회원 행을 지운다.
    // 메시지는 outbox 에 함께 커밋되므로, 삭제가 롤백되면 메시지도 나가지 않고 다음 스케줄에 다시 시도한다
    @Transactional
    override fun purge(memberId: Long) {
        val finalAttemptCutoff = LocalDateTime.now(clock)
            .minusDays(WITHDRAWAL_RETENTION_DAYS + PURGE_RETRY_DAYS)
        val member = memberPort.findById(memberId)
            ?.takeIf { it.isWithdrawn() }
            ?: return
        eventPublisher.publishEvent(
            MemberPurgedEvent(
                memberId,
                member.provider,
                member.providerMemberId,
                member.deletedAt!!.isBefore(finalAttemptCutoff),
            ),
        )
        memberEventPublisher.purged(memberId)
        memberPort.delete(member)
    }

    companion object {
        // 탈퇴 후 회원 행과 데이터를 보관하는 기간. 이 기간 동안은 같은 계정으로 재가입할 수 없다
        private const val WITHDRAWAL_RETENTION_DAYS = 30L

        // 보관 기간이 지나고 매일 삭제를 시도해도 이만큼 실패하면, 포기할 수 있는 작업(소셜 연결 끊기)은 건너뛰고 삭제한다
        private const val PURGE_RETRY_DAYS = 3L
    }
}

package ijiri.ijiriserver.domain.member.application.service

import ijiri.ijiriserver.domain.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.FindMemberPurgeTargetsUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.PurgeMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.SuspendMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.WithdrawMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.out.MemberPort
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.domain.member.event.MemberSuspendedEvent
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent
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
    private val clock: Clock,
    private val getMemberUseCase: GetMemberUseCase,
    private val findActiveMemberUseCase: FindActiveMemberUseCase,
) : WithdrawMemberUseCase,
    SuspendMemberUseCase,
    FindMemberPurgeTargetsUseCase,
    PurgeMemberUseCase {

    // soft delete: 행은 보관 기간 동안 남기고, 즉시 처리할 일(토큰 폐기, 소셜 연결 끊기, 게시물 비공개 등)은 이벤트로 넘긴다.
    // 소셜 연결 끊기처럼 외부 API 를 부르는 일은 커밋 뒤에 실행되고, 실패하면 영구 삭제 전에 다시 시도한다
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

    // 다른 도메인이 자기 데이터를 먼저 지우도록 이벤트를 발행한 뒤 회원 행을 삭제한다
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
        memberPort.delete(member)
    }

    companion object {
        // 탈퇴 후 회원 행과 데이터를 보관하는 기간. 이 기간 동안은 같은 계정으로 재가입할 수 없다
        private const val WITHDRAWAL_RETENTION_DAYS = 30L

        // 보관 기간이 지나고 매일 삭제를 시도해도 이만큼 실패하면, 포기할 수 있는 작업(소셜 연결 끊기)은 건너뛰고 삭제한다
        private const val PURGE_RETRY_DAYS = 3L
    }
}

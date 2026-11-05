package ijiri.ijiriserver.user.auth.common.application.service

import ijiri.ijiriserver.user.auth.common.application.port.`in`.UnlinkSocialAfterWithdrawalUseCase
import ijiri.ijiriserver.user.auth.common.application.port.`in`.UnlinkSocialBeforePurgeUseCase
import ijiri.ijiriserver.user.auth.common.application.port.out.SocialUnlinkClient
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import ijiri.ijiriserver.user.member.event.MemberWithdrawnEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 탈퇴한 회원의 소셜 계정 연결 끊기. 외부 API 응답을 기다리는 동안 DB 커넥션을 잡지 않도록
 * 탈퇴 커밋 뒤에 호출하고, 실패하면 영구 삭제 직전에 다시 시도한다 (이미 끊긴 계정은 성공으로 처리되므로 멱등).
 */
@Service
class SocialUnlinkService(
    clients: List<SocialUnlinkClient>,
) : UnlinkSocialAfterWithdrawalUseCase,
    UnlinkSocialBeforePurgeUseCase {

    private val clients: Map<Provider, SocialUnlinkClient> = clients.associateBy { it.provider() }

    // 탈퇴는 이미 커밋됐으므로 실패해도 응답을 바꾸지 않고, 영구 삭제 때 재시도한다
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    override fun unlinkAfterWithdrawal(event: MemberWithdrawnEvent) {
        try {
            unlink(event.provider, event.providerMemberId)
        } catch (e: RuntimeException) {
            log.warn(
                "Social unlink failed after withdrawal, will retry before purge: memberId={}",
                event.memberId,
                e,
            )
        }
    }

    // 실패하면 예외로 영구 삭제가 롤백되고 다음 스케줄에 다시 시도된다.
    // 며칠째 실패한 마지막 시도라면 연결 끊기를 포기하고 삭제를 진행한다 (30일 삭제 약속이 우선)
    // 되돌릴 수 없는 외부 호출이 실패하면 다른 리스너가 일을 하기 전에 멈추도록 영구 삭제 리스너 중 가장 먼저 실행한다
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    override fun unlinkBeforePurge(event: MemberPurgedEvent) {
        try {
            unlink(event.provider, event.providerMemberId)
        } catch (e: RuntimeException) {
            if (!event.finalAttempt) {
                throw e
            }
            log.error(
                "Social unlink gave up, purging anyway: memberId={}, provider={}",
                event.memberId,
                event.provider,
                e,
            )
        }
    }

    // 연결 끊기가 필요 없는 provider(구글, 이메일)는 구현체가 없으므로 아무것도 하지 않는다
    private fun unlink(provider: Provider, providerMemberId: String) {
        clients[provider]?.unlink(providerMemberId)
    }

    companion object {
        private val log = LoggerFactory.getLogger(SocialUnlinkService::class.java)
    }
}

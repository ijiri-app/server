package ijiri.ijiriserver.domain.auth.common.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialUnlinkClient;
import ijiri.ijiriserver.domain.auth.common.service.SocialUnlinkService;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 탈퇴한 회원의 소셜 계정 연결 끊기. 외부 API 응답을 기다리는 동안 DB 커넥션을 잡지 않도록
 * 탈퇴 커밋 뒤에 호출하고, 실패하면 영구 삭제 직전에 다시 시도한다 (이미 끊긴 계정은 성공으로 처리되므로 멱등).
 */
@Slf4j
@Service
public class SocialUnlinkServiceImpl implements SocialUnlinkService {

    private final Map<Provider, SocialUnlinkClient> clients;

    public SocialUnlinkServiceImpl(List<SocialUnlinkClient> clients) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(SocialUnlinkClient::provider, Function.identity()));
    }

    // 탈퇴는 이미 커밋됐으므로 실패해도 응답을 바꾸지 않고, 영구 삭제 때 재시도한다
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void unlinkAfterWithdrawal(MemberWithdrawnEvent event) {
        try {
            unlink(event.provider(), event.providerMemberId());
        } catch (RuntimeException e) {
            log.warn(
                    "Social unlink failed after withdrawal, will retry before purge: memberId={}",
                    event.memberId(),
                    e
            );
        }
    }

    // 실패하면 예외로 영구 삭제가 롤백되고 다음 스케줄에 다시 시도된다.
    // 며칠째 실패한 마지막 시도라면 연결 끊기를 포기하고 삭제를 진행한다 (30일 삭제 약속이 우선)
    @Override
    @EventListener
    public void unlinkBeforePurge(MemberPurgedEvent event) {
        try {
            unlink(event.provider(), event.providerMemberId());
        } catch (RuntimeException e) {
            if (!event.finalAttempt()) {
                throw e;
            }
            log.error(
                    "Social unlink gave up, purging anyway: memberId={}, provider={}",
                    event.memberId(),
                    event.provider(),
                    e
            );
        }
    }

    // 연결 끊기가 필요 없는 provider(구글, 이메일)는 구현체가 없으므로 아무것도 하지 않는다
    private void unlink(Provider provider, String providerMemberId) {
        SocialUnlinkClient client = clients.get(provider);
        if (client != null) {
            client.unlink(providerMemberId);
        }
    }
}

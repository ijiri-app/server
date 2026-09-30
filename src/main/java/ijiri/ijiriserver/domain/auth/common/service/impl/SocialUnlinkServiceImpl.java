package ijiri.ijiriserver.domain.auth.common.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialUnlinkClient;
import ijiri.ijiriserver.domain.auth.common.service.SocialUnlinkService;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SocialUnlinkServiceImpl implements SocialUnlinkService {

    private final Map<Provider, SocialUnlinkClient> clients;

    public SocialUnlinkServiceImpl(List<SocialUnlinkClient> clients) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(SocialUnlinkClient::provider, Function.identity()));
    }

    // 탈퇴 트랜잭션의 커밋 직전에 실행: 다른 정리 작업이 모두 끝난 뒤 외부 API 를 호출하고,
    // 실패하면 예외로 탈퇴 전체가 롤백된다
    @Override
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void unlink(MemberWithdrawnEvent event) {
        // 연결 끊기가 필요 없는 provider(구글, 이메일)는 구현체가 없으므로 아무것도 하지 않는다
        SocialUnlinkClient client = clients.get(event.provider());
        if (client != null) {
            client.unlink(event.providerMemberId());
        }
    }
}

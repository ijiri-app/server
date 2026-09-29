package ijiri.ijiriserver.domain.auth.common.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialUnlinkClient;
import ijiri.ijiriserver.domain.auth.common.service.SocialUnlinkService;
import ijiri.ijiriserver.domain.member.entity.Provider;
import org.springframework.stereotype.Service;

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

    @Override
    public void unlink(Provider provider, String providerMemberId) {
        // 연결 끊기가 필요 없는 provider(구글 등)는 구현체가 없으므로 아무것도 하지 않는다
        SocialUnlinkClient client = clients.get(provider);
        if (client != null) {
            client.unlink(providerMemberId);
        }
    }
}

package ijiri.ijiriserver.domain.auth.common.service;

import ijiri.ijiriserver.domain.member.entity.Provider;

public interface SocialUnlinkService {

    void unlink(Provider provider, String providerMemberId);
}

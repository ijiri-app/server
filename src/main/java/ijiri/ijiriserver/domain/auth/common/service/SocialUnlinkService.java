package ijiri.ijiriserver.domain.auth.common.service;

import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;

public interface SocialUnlinkService {

    void unlink(MemberWithdrawnEvent event);
}

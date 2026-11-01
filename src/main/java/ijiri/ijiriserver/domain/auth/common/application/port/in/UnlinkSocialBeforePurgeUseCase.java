package ijiri.ijiriserver.domain.auth.common.application.port.in;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;

public interface UnlinkSocialBeforePurgeUseCase {

    void unlinkBeforePurge(MemberPurgedEvent event);
}

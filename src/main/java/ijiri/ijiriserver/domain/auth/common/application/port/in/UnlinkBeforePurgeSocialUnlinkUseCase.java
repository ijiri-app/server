package ijiri.ijiriserver.domain.auth.common.application.port.in;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;

public interface UnlinkBeforePurgeSocialUnlinkUseCase {

    void unlinkBeforePurge(MemberPurgedEvent event);
}

package ijiri.ijiriserver.domain.auth.common.application.port.in;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;

public interface SocialUnlinkUseCase {

    void unlinkAfterWithdrawal(MemberWithdrawnEvent event);

    void unlinkBeforePurge(MemberPurgedEvent event);
}

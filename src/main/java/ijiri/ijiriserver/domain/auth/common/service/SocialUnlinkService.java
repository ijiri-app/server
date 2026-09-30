package ijiri.ijiriserver.domain.auth.common.service;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;

public interface SocialUnlinkService {

    void unlinkAfterWithdrawal(MemberWithdrawnEvent event);

    void unlinkBeforePurge(MemberPurgedEvent event);
}

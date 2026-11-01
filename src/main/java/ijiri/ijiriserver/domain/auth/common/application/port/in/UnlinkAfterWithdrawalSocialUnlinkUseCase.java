package ijiri.ijiriserver.domain.auth.common.application.port.in;

import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;

public interface UnlinkAfterWithdrawalSocialUnlinkUseCase {

    void unlinkAfterWithdrawal(MemberWithdrawnEvent event);
}

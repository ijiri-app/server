package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.application.data.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.application.data.MemberRegisterResult;

public interface RegisterIfAbsentMemberUseCase {

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);
}

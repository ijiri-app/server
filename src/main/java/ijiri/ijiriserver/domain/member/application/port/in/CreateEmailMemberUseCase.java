package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.application.data.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.entity.Member;

public interface CreateEmailMemberUseCase {

    Member signup(MemberSignupCommand command);
}

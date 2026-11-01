package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.entity.Member;

import java.util.Collection;
import java.util.List;

public interface GetActiveMembersUseCase {

    List<Member> getActiveMembers(Collection<Long> memberIds);
}

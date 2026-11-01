package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.entity.Member;

public interface GetMemberUseCase {

    Member getById(Long memberId);
}

package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.entity.Member;

public interface GetByIdMemberUseCase {

    Member getById(Long memberId);
}

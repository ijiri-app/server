package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.entity.Member;

import java.util.Optional;

public interface FindEmailMemberUseCase {

    Optional<Member> findEmailMember(String email);
}

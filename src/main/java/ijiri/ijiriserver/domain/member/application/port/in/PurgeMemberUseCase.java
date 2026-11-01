package ijiri.ijiriserver.domain.member.application.port.in;

public interface PurgeMemberUseCase {

    void purge(Long memberId);
}

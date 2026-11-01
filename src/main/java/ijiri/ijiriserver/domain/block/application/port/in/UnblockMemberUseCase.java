package ijiri.ijiriserver.domain.block.application.port.in;

public interface UnblockMemberUseCase {

    void unblock(Long blockerId, Long targetId);
}

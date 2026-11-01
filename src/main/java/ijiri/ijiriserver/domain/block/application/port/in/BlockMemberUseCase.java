package ijiri.ijiriserver.domain.block.application.port.in;

public interface BlockMemberUseCase {

    void block(Long blockerId, Long targetId);
}

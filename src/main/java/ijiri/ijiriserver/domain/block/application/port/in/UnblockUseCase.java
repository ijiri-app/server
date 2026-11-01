package ijiri.ijiriserver.domain.block.application.port.in;

public interface UnblockUseCase {

    void unblock(Long blockerId, Long targetId);
}

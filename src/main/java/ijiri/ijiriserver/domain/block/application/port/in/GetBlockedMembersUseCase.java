package ijiri.ijiriserver.domain.block.application.port.in;

import ijiri.ijiriserver.domain.block.application.data.response.BlockResponse;

public interface GetBlockedMembersUseCase {

    BlockResponse getBlockedMembers(Long memberId);
}

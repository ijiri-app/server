package ijiri.ijiriserver.domain.block.service;

import ijiri.ijiriserver.domain.block.dto.response.BlockResponse;

import java.util.Set;

public interface BlockService {

    BlockResponse block(Long blockerId, Long targetId);

    BlockResponse unblock(Long blockerId, Long targetId);

    BlockResponse getBlockedMembers(Long memberId);

    /**
     * memberId 가 차단했거나 memberId 를 차단한 회원들. 서로의 게시물을 보여주지 않는 데 쓴다.
     */
    Set<Long> getHiddenMemberIds(Long memberId);
}

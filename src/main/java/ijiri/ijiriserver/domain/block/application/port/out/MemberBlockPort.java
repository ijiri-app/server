package ijiri.ijiriserver.domain.block.application.port.out;

import ijiri.ijiriserver.domain.block.entity.MemberBlock;

import java.util.List;
import java.util.Optional;

public interface MemberBlockPort {

    Optional<MemberBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<MemberBlock> findAllByBlockerIdOrderByIdDesc(Long blockerId);

    List<Long> findRelatedMemberIds(Long memberId);

    void deleteAllRelatedTo(Long memberId);

    MemberBlock save(MemberBlock entity);

    void delete(MemberBlock entity);
}

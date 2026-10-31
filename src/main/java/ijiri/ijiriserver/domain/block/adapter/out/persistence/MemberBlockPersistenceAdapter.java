package ijiri.ijiriserver.domain.block.adapter.out.persistence;

import ijiri.ijiriserver.domain.block.application.port.out.MemberBlockPort;
import ijiri.ijiriserver.domain.block.entity.MemberBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberBlockPersistenceAdapter implements MemberBlockPort {

    private final MemberBlockJpaRepository memberBlockJpaRepository;

    @Override
    public Optional<MemberBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return memberBlockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public List<MemberBlock> findAllByBlockerIdOrderByIdDesc(Long blockerId) {
        return memberBlockJpaRepository.findAllByBlockerIdOrderByIdDesc(blockerId);
    }

    @Override
    public List<Long> findRelatedMemberIds(Long memberId) {
        return memberBlockJpaRepository.findRelatedMemberIds(memberId);
    }

    @Override
    public void deleteAllRelatedTo(Long memberId) {
        memberBlockJpaRepository.deleteAllRelatedTo(memberId);
    }

    @Override
    public MemberBlock save(MemberBlock entity) {
        return memberBlockJpaRepository.save(entity);
    }

    @Override
    public void delete(MemberBlock entity) {
        memberBlockJpaRepository.delete(entity);
    }
}

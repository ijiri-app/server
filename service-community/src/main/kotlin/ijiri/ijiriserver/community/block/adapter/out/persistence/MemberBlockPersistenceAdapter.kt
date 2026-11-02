package ijiri.ijiriserver.community.block.adapter.out.persistence

import ijiri.ijiriserver.community.block.application.port.out.MemberBlockPort
import ijiri.ijiriserver.community.block.entity.MemberBlock
import org.springframework.stereotype.Component

@Component
class MemberBlockPersistenceAdapter(
    private val memberBlockJpaRepository: MemberBlockJpaRepository,
) : MemberBlockPort {

    override fun findByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): MemberBlock? {
        return memberBlockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
    }

    override fun findAllByBlockerIdOrderByIdDesc(blockerId: Long): List<MemberBlock> {
        return memberBlockJpaRepository.findAllByBlockerIdOrderByIdDesc(blockerId)
    }

    override fun findRelatedMemberIds(memberId: Long): List<Long> {
        return memberBlockJpaRepository.findRelatedMemberIds(memberId)
    }

    override fun deleteAllRelatedTo(memberId: Long) {
        memberBlockJpaRepository.deleteAllRelatedTo(memberId)
    }

    override fun save(entity: MemberBlock): MemberBlock {
        return memberBlockJpaRepository.save(entity)
    }

    override fun delete(entity: MemberBlock) {
        memberBlockJpaRepository.delete(entity)
    }
}

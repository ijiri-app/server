package ijiri.ijiriserver.domain.block.application.port.out

import ijiri.ijiriserver.domain.block.entity.MemberBlock

interface MemberBlockPort {

    fun findByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): MemberBlock?

    fun findAllByBlockerIdOrderByIdDesc(blockerId: Long): List<MemberBlock>

    fun findRelatedMemberIds(memberId: Long): List<Long>

    fun deleteAllRelatedTo(memberId: Long)

    fun save(entity: MemberBlock): MemberBlock

    fun delete(entity: MemberBlock)
}

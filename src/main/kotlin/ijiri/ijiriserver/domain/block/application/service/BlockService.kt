package ijiri.ijiriserver.domain.block.application.service

import ijiri.ijiriserver.domain.block.application.data.response.BlockResponse
import ijiri.ijiriserver.domain.block.application.port.`in`.BlockMemberUseCase
import ijiri.ijiriserver.domain.block.application.port.`in`.GetBlockedMembersUseCase
import ijiri.ijiriserver.domain.block.application.port.`in`.GetHiddenMemberIdsUseCase
import ijiri.ijiriserver.domain.block.application.port.`in`.UnblockMemberUseCase
import ijiri.ijiriserver.domain.block.application.port.out.MemberBlockPort
import ijiri.ijiriserver.domain.block.entity.MemberBlock
import ijiri.ijiriserver.domain.block.exception.BlockStatusCode
import ijiri.ijiriserver.domain.member.application.port.`in`.GetActiveMembersUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlockService(
    private val memberBlockPort: MemberBlockPort,
    private val getActiveMembersUseCase: GetActiveMembersUseCase,
    private val getMemberUseCase: GetMemberUseCase,
) : BlockMemberUseCase,
    UnblockMemberUseCase,
    GetBlockedMembersUseCase,
    GetHiddenMemberIdsUseCase {

    // 이미 차단한 회원이면 그대로 성공으로 응답한다
    @Transactional
    override fun block(blockerId: Long, targetId: Long) {
        if (blockerId == targetId) {
            throw CustomException(BlockStatusCode.CANNOT_BLOCK_SELF)
        }
        getMemberUseCase.getById(blockerId)
        getMemberUseCase.getById(targetId)
        if (memberBlockPort.findByBlockerIdAndBlockedId(blockerId, targetId) == null) {
            memberBlockPort.save(
                MemberBlock(
                    blockerId = blockerId,
                    blockedId = targetId,
                ),
            )
        }
    }

    @Transactional
    override fun unblock(blockerId: Long, targetId: Long) {
        memberBlockPort.findByBlockerIdAndBlockedId(blockerId, targetId)
            ?.let { memberBlockPort.delete(it) }
    }

    // 차단한 뒤 탈퇴한 회원은 목록에서 뺀다
    override fun getBlockedMembers(memberId: Long): BlockResponse {
        val blockedIds = memberBlockPort.findAllByBlockerIdOrderByIdDesc(memberId)
            .map { it.blockedId }
        val members = getActiveMembersUseCase.getActiveMembers(blockedIds)
            .associateBy { it.id }
        return BlockResponse(
            blockedIds
                .mapNotNull { members[it] }
                .map { member ->
                    BlockResponse.BlockedMember(
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                    )
                },
        )
    }

    override fun getHiddenMemberIds(memberId: Long): Set<Long> {
        return memberBlockPort.findRelatedMemberIds(memberId).toHashSet()
    }

    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        memberBlockPort.deleteAllRelatedTo(event.memberId)
    }
}

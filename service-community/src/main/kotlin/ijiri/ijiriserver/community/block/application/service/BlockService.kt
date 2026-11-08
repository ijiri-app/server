package ijiri.ijiriserver.community.block.application.service

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.community.block.application.data.response.BlockResponse
import ijiri.ijiriserver.community.block.application.port.`in`.BlockMemberUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.DeleteMemberBlocksUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.GetBlockedMembersUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.GetHiddenMemberIdsUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.UnblockMemberUseCase
import ijiri.ijiriserver.community.block.application.port.out.MemberBlockPort
import ijiri.ijiriserver.community.block.entity.MemberBlock
import ijiri.ijiriserver.community.block.exception.BlockStatusCode
import ijiri.ijiriserver.community.post.application.port.out.MemberReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlockService(
    private val memberBlockPort: MemberBlockPort,
    private val memberReader: MemberReader,
) : BlockMemberUseCase,
    UnblockMemberUseCase,
    GetBlockedMembersUseCase,
    GetHiddenMemberIdsUseCase,
    DeleteMemberBlocksUseCase {

    // 이미 차단한 회원이면 그대로 성공으로 응답한다. 차단하는 쪽은 게이트웨이가 활성 회원임을 확인했다
    @Transactional
    override fun block(blockerId: Long, targetId: Long) {
        if (blockerId == targetId) {
            throw CustomException(BlockStatusCode.CANNOT_BLOCK_SELF)
        }
        memberReader.findActive(targetId) ?: throw CustomException(BlockStatusCode.MEMBER_NOT_FOUND)
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
        val members = memberReader.getActive(blockedIds)
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

    @Transactional
    override fun deleteAll(memberId: Long) {
        memberBlockPort.deleteAllRelatedTo(memberId)
    }
}

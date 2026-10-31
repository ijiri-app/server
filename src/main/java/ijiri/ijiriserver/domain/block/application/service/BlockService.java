package ijiri.ijiriserver.domain.block.application.service;

import ijiri.ijiriserver.domain.block.application.data.response.BlockResponse;
import ijiri.ijiriserver.domain.block.application.port.in.BlockUseCase;
import ijiri.ijiriserver.domain.block.application.port.out.MemberBlockPort;
import ijiri.ijiriserver.domain.block.entity.MemberBlock;
import ijiri.ijiriserver.domain.block.exception.BlockStatusCode;
import ijiri.ijiriserver.domain.member.application.port.in.MemberUseCase;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService implements BlockUseCase {

    private final MemberBlockPort memberBlockPort;
    private final MemberUseCase memberUseCase;

    // 이미 차단한 회원이면 그대로 성공으로 응답한다
    @Override
    @Transactional
    public void block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new CustomException(BlockStatusCode.CANNOT_BLOCK_SELF);
        }
        memberUseCase.getById(blockerId);
        memberUseCase.getById(targetId);
        if (memberBlockPort.findByBlockerIdAndBlockedId(blockerId, targetId).isEmpty()) {
            memberBlockPort.save(MemberBlock.builder()
                    .blockerId(blockerId)
                    .blockedId(targetId)
                    .build()
            );
        }
    }

    @Override
    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        memberBlockPort.findByBlockerIdAndBlockedId(blockerId, targetId)
                .ifPresent(memberBlockPort::delete);
    }

    // 차단한 뒤 탈퇴한 회원은 목록에서 뺀다
    @Override
    public BlockResponse getBlockedMembers(Long memberId) {
        List<Long> blockedIds = memberBlockPort.findAllByBlockerIdOrderByIdDesc(memberId).stream()
                .map(MemberBlock::getBlockedId)
                .toList();
        Map<Long, Member> members = memberUseCase.getActiveMembers(blockedIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        return new BlockResponse(blockedIds.stream()
                .map(members::get)
                .filter(Objects::nonNull)
                .map(member -> new BlockResponse.BlockedMember(
                        member.getId(),
                        member.getNickname(),
                        member.getProfileImageUrl()
                ))
                .toList()
        );
    }

    @Override
    public Set<Long> getHiddenMemberIds(Long memberId) {
        return new HashSet<>(memberBlockPort.findRelatedMemberIds(memberId));
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        memberBlockPort.deleteAllRelatedTo(event.memberId());
    }
}

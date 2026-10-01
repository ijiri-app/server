package ijiri.ijiriserver.domain.block.controller;

import ijiri.ijiriserver.domain.block.dto.response.BlockResponse;
import ijiri.ijiriserver.domain.block.exception.BlockStatusCode;
import ijiri.ijiriserver.domain.block.service.BlockService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Block", description = "사용자 차단")
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @Operation(
            summary = "사용자 차단",
            description = "차단하면 서로의 게시물이 피드, 상세, 게시물 목록에서 즉시 보이지 않는다. 이미 차단했어도 성공"
    )
    @PostMapping("/{memberId}/block")
    public BaseResponse<BlockResponse> block(
            @Parameter(hidden = true) @AuthenticationPrincipal String blockerId,
            @PathVariable Long memberId
    ) {
        return BaseResponse.of(BlockStatusCode.BLOCK_SUCCESS, blockService.block(Long.valueOf(blockerId), memberId));
    }

    @Operation(
            summary = "차단 해제",
            description = "차단하지 않은 사용자여도 성공"
    )
    @DeleteMapping("/{memberId}/block")
    public BaseResponse<BlockResponse> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal String blockerId,
            @PathVariable Long memberId
    ) {
        return BaseResponse.of(
                BlockStatusCode.UNBLOCK_SUCCESS,
                blockService.unblock(Long.valueOf(blockerId), memberId)
        );
    }

    @Operation(
            summary = "내 차단 목록",
            description = "설정 화면에서 차단 해제에 사용. 최근 차단순"
    )
    @GetMapping("/me/blocks")
    public BaseResponse<BlockResponse> getBlockedMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId
    ) {
        return BaseResponse.ok(blockService.getBlockedMembers(Long.valueOf(memberId)));
    }
}

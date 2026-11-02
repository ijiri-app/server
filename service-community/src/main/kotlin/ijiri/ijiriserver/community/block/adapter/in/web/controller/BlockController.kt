package ijiri.ijiriserver.community.block.adapter.`in`.web.controller

import ijiri.ijiriserver.community.block.application.data.response.BlockResponse
import ijiri.ijiriserver.community.block.application.port.`in`.BlockMemberUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.GetBlockedMembersUseCase
import ijiri.ijiriserver.community.block.application.port.`in`.UnblockMemberUseCase
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Block", description = "사용자 차단")
@RestController
@RequestMapping("/members")
class BlockController(
    private val blockMemberUseCase: BlockMemberUseCase,
    private val getBlockedMembersUseCase: GetBlockedMembersUseCase,
    private val unblockMemberUseCase: UnblockMemberUseCase,
) {

    @Operation(
        summary = "사용자 차단",
        description = "차단하면 서로의 게시물이 피드, 상세, 게시물 목록에서 즉시 보이지 않는다. 이미 차단했어도 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{memberId}/block")
    fun block(
        @Parameter(hidden = true) @AuthenticationPrincipal blockerId: String,
        @PathVariable memberId: Long,
    ) {
        blockMemberUseCase.block(blockerId.toLong(), memberId)
    }

    @Operation(
        summary = "차단 해제",
        description = "차단하지 않은 사용자여도 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{memberId}/block")
    fun unblock(
        @Parameter(hidden = true) @AuthenticationPrincipal blockerId: String,
        @PathVariable memberId: Long,
    ) {
        unblockMemberUseCase.unblock(blockerId.toLong(), memberId)
    }

    @Operation(
        summary = "내 차단 목록",
        description = "설정 화면에서 차단 해제에 사용. 최근 차단순",
    )
    @GetMapping("/me/blocks")
    fun getBlockedMembers(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
    ): BaseResponse<BlockResponse> {
        return BaseResponse.ok(getBlockedMembersUseCase.getBlockedMembers(memberId.toLong()))
    }
}

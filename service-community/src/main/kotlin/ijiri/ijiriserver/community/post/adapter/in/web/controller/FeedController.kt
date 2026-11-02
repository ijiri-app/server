package ijiri.ijiriserver.community.post.adapter.`in`.web.controller

import ijiri.ijiriserver.common.contract.BuildStyle
import ijiri.ijiriserver.community.post.application.data.response.PostResponse
import ijiri.ijiriserver.community.post.application.port.`in`.GetFeedUseCase
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Feed", description = "피드")
@RestController
class FeedController(
    private val getFeedUseCase: GetFeedUseCase,
) {

    @Operation(
        summary = "피드",
        description = "최신순 카드(썸네일 크기 포함). 차종 탭은 carModelId, 비로그인 '모두' 탭은 기기에 저장한 " +
            "carModelIds(쉼표 구분). 로그인 상태에서 둘 다 없으면 내 관심 차종 전체, 관심 차종도 없으면 전체. " +
            "buildStyle 은 빌드 방향 칩. 차단 관계인 회원과 숨김 게시물은 뺀다. cursor 는 이전 응답의 nextCursor",
    )
    @SecurityRequirements
    @GetMapping("/feed")
    fun getFeed(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String?,
        @RequestParam(required = false) carModelId: Long?,
        @RequestParam(required = false) @Size(max = 30) carModelIds: List<Long>?,
        @RequestParam(required = false) buildStyle: BuildStyle?,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): BaseResponse<PostResponse> =
        BaseResponse.ok(
            getFeedUseCase.getFeed(
                memberId?.toLong(),
                carModelId,
                carModelIds.orEmpty(),
                buildStyle,
                cursor,
                size,
            )
        )
}

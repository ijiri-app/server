package ijiri.ijiriserver.domain.post.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.post.application.data.request.PostCreateRequest
import ijiri.ijiriserver.domain.post.application.data.request.PostUpdateRequest
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse
import ijiri.ijiriserver.domain.post.application.port.`in`.CreatePostUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.DeletePostUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetMemberPostsUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.GetPostUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.UpdatePostUseCase
import ijiri.ijiriserver.domain.post.exception.PostStatusCode
import ijiri.ijiriserver.global.data.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Post", description = "게시물")
@RestController
class PostController(
    private val createPostUseCase: CreatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getMemberPostsUseCase: GetMemberPostsUseCase,
    private val getPostUseCase: GetPostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
) {

    @Operation(
        summary = "게시물 올리기",
        description = "업로드한 사진(imageKey, width, height) 1~10장, 내 보유 차량(남의 차량이면 FORBIDDEN), 빌드 방향, " +
            "본문(1000자), 부품 0개 이상(기존 partId 또는 brandName + partName). " +
            "태그는 사진마다 ref 로 부품을 가리키며 x, y 는 0~1 비율. 201 { id }",
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/posts")
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: PostCreateRequest,
    ): BaseResponse<PostResponse> =
        BaseResponse.of(
            PostStatusCode.CREATE_SUCCESS,
            createPostUseCase.create(memberId.toLong(), request),
        )

    @Operation(
        summary = "게시물 상세",
        description = "작성자·차량, 본문, 사진(태그 위치), 부품(분류순, 담기 수, 내 담기 여부), 전체 담기 수. " +
            "숨김·삭제된 게시물, 차단 관계인 회원의 게시물은 NOT_FOUND",
    )
    @SecurityRequirements
    @GetMapping("/posts/{postId}")
    fun getPost(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String?,
        @PathVariable postId: Long,
    ): BaseResponse<PostResponse> =
        BaseResponse.ok(getPostUseCase.getPost(toMemberId(memberId), postId))

    @Operation(
        summary = "게시물 수정",
        description = "본문, 빌드 방향, 부품·태그(사진 교체는 안 됨). 보낸 필드만 바뀐다. " +
            "부품을 보내면 전체 교체이며, 유지할 부품은 postPartId 를 같이 보내야 담기가 남는다. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/posts/{postId}")
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @PathVariable postId: Long,
        @Valid @RequestBody request: PostUpdateRequest,
    ) {
        updatePostUseCase.update(memberId.toLong(), postId, request)
    }

    @Operation(
        summary = "게시물 삭제",
        description = "사진 파일과 이 게시물 부품의 담기까지 삭제. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/posts/{postId}")
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @PathVariable postId: Long,
    ) {
        deletePostUseCase.delete(memberId.toLong(), postId)
    }

    @Operation(
        summary = "회원의 게시물 목록",
        description = "피드와 같은 카드, 최신순. 로그인 없이도 볼 수 있다. " +
            "본인 목록에는 숨김 처리된 게시물도 hidden = true 로 포함",
    )
    @SecurityRequirements
    @GetMapping("/members/{memberId}/posts")
    fun getMemberPosts(
        @Parameter(hidden = true) @AuthenticationPrincipal viewerId: String?,
        @PathVariable memberId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): BaseResponse<PostResponse> =
        BaseResponse.ok(getMemberPostsUseCase.getMemberPosts(toMemberId(viewerId), memberId, cursor, size))

    // 공개 API 라 비로그인이면 principal 이 null 이다
    private fun toMemberId(memberId: String?): Long? = memberId?.toLong()
}

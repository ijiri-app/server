package ijiri.ijiriserver.domain.post.controller;

import ijiri.ijiriserver.domain.post.dto.request.PostCreateRequest;
import ijiri.ijiriserver.domain.post.dto.request.PostUpdateRequest;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;
import ijiri.ijiriserver.domain.post.exception.PostStatusCode;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post", description = "게시물")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시물 올리기",
            description = "사진(업로드한 imageId) 1~10장과 사진별 부품 태그, 보유 차량, 빌드 방향, 본문. "
                    + "검색에 없는 부품·브랜드는 태그에 이름을 넣으면 함께 등록된다"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/posts")
    public BaseResponse<PostResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return BaseResponse.of(PostStatusCode.CREATE_SUCCESS, postService.create(Long.valueOf(memberId), request));
    }

    @Operation(
            summary = "게시물 상세",
            description = "본문, 사진(태그 위치 포함), 분류별 부품 목록. 로그인하면 부품별 위시리스트 여부도 준다"
    )
    @SecurityRequirements
    @GetMapping("/posts/{postId}")
    public BaseResponse<PostResponse> getPost(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long postId
    ) {
        return BaseResponse.ok(postService.getPost(toMemberId(memberId), postId));
    }

    @Operation(
            summary = "게시물 수정",
            description = "보낸 필드만 바뀐다. images 를 보내면 사진·태그 전체 교체"
    )
    @PatchMapping("/posts/{postId}")
    public BaseResponse<PostResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return BaseResponse.of(
                PostStatusCode.UPDATE_SUCCESS,
                postService.update(Long.valueOf(memberId), postId, request)
        );
    }

    @Operation(
            summary = "게시물 삭제",
            description = "사진 파일까지 삭제"
    )
    @DeleteMapping("/posts/{postId}")
    public BaseResponse<PostResponse> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long postId
    ) {
        return BaseResponse.of(PostStatusCode.DELETE_SUCCESS, postService.delete(Long.valueOf(memberId), postId));
    }

    @Operation(
            summary = "회원의 게시물 목록",
            description = "최신순 카드 목록. 본인 목록에는 숨김 처리된 게시물도 status 와 함께 포함"
    )
    @SecurityRequirements
    @GetMapping("/members/{memberId}/posts")
    public BaseResponse<PostResponse> getMemberPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal String viewerId,
            @PathVariable Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(postService.getMemberPosts(toMemberId(viewerId), memberId, cursor, size));
    }

    // 공개 API 라 비로그인이면 principal 이 null 이다
    private Long toMemberId(String memberId) {
        return memberId != null ? Long.valueOf(memberId) : null;
    }
}

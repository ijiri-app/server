package ijiri.ijiriserver.domain.post.controller;

import ijiri.ijiriserver.domain.carmodel.entity.BuildDirection;
import ijiri.ijiriserver.domain.post.dto.response.PostResponse;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Feed", description = "피드")
@RestController
@RequiredArgsConstructor
public class FeedController {

    private final PostService postService;

    @Operation(
            summary = "피드",
            description = "최신순 카드 목록(무한 스크롤: cursor 에 이전 응답의 nextCursor). "
                    + "관심 차종 탭: '모두'는 관심 차종 ID 전체, 차종 탭은 그 ID 하나를 carModelIds 로 보낸다(비우면 전체). "
                    + "buildDirection 은 빌드 방향 칩. 로그인하면 차단 관계인 회원의 게시물을 뺀다"
    )
    @SecurityRequirements
    @GetMapping("/feed")
    public BaseResponse<PostResponse> getFeed(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestParam(required = false) @Size(max = 30) List<Long> carModelIds,
            @RequestParam(required = false) BuildDirection buildDirection,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(postService.getFeed(
                memberId != null ? Long.valueOf(memberId) : null,
                carModelIds != null ? carModelIds : List.of(),
                buildDirection,
                cursor,
                size
        ));
    }
}

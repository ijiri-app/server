package ijiri.ijiriserver.domain.post.controller;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
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
            description = "최신순 카드(썸네일 크기 포함). 차종 탭은 carModelId, 비로그인 '모두' 탭은 기기에 저장한 "
                    + "carModelIds(쉼표 구분). 로그인 상태에서 둘 다 없으면 내 관심 차종 전체, 관심 차종도 없으면 전체. "
                    + "buildStyle 은 빌드 방향 칩. 차단 관계인 회원과 숨김 게시물은 뺀다. cursor 는 이전 응답의 nextCursor"
    )
    @SecurityRequirements
    @GetMapping("/feed")
    public BaseResponse<PostResponse> getFeed(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestParam(required = false) Long carModelId,
            @RequestParam(required = false) @Size(max = 30) List<Long> carModelIds,
            @RequestParam(required = false) BuildStyle buildStyle,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(postService.getFeed(
                memberId != null ? Long.valueOf(memberId) : null,
                carModelId,
                carModelIds != null ? carModelIds : List.of(),
                buildStyle,
                cursor,
                size
        ));
    }
}

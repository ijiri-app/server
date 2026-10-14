package ijiri.ijiriserver.domain.wishlist.controller;

import ijiri.ijiriserver.domain.wishlist.dto.WishlistAddResult;
import ijiri.ijiriserver.domain.wishlist.dto.request.WishlistAddRequest;
import ijiri.ijiriserver.domain.wishlist.dto.response.WishlistResponse;
import ijiri.ijiriserver.domain.wishlist.exception.WishlistStatusCode;
import ijiri.ijiriserver.domain.wishlist.service.WishlistService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wishlist", description = "위시리스트")
@RestController
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "부품 담기",
            description = "게시물 부품(postPartId)을 담는다. 새로 담으면 201, 이미 담겨 있으면 200 과 같은 id. "
                    + "숨김·삭제된 게시물이면 NOT_FOUND"
    )
    @PostMapping("/wishlist")
    public ResponseEntity<BaseResponse<WishlistResponse>> add(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody WishlistAddRequest request
    ) {
        WishlistAddResult result = wishlistService.add(Long.valueOf(memberId), request.postPartId());
        WishlistStatusCode statusCode = result.created()
                ? WishlistStatusCode.ADD_SUCCESS
                : WishlistStatusCode.ALREADY_ADDED;
        return ResponseEntity.status(statusCode.getHttpStatus())
                .body(BaseResponse.of(statusCode, WishlistResponse.added(result.id())));
    }

    @Operation(
            summary = "부품 빼기",
            description = "id 는 위시리스트 항목 ID. 204"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/wishlist/{wishlistItemId}")
    public void remove(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long wishlistItemId
    ) {
        wishlistService.remove(Long.valueOf(memberId), wishlistItemId);
    }

    @Operation(
            summary = "내 위시리스트",
            description = "최근 담은 순. 원 게시물이 숨김·삭제되면 빠진다. cursor 는 이전 응답의 nextCursor"
    )
    @GetMapping("/members/me/wishlist")
    public BaseResponse<WishlistResponse> getWishlist(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return BaseResponse.ok(wishlistService.getWishlist(Long.valueOf(memberId), cursor, size));
    }
}

package ijiri.ijiriserver.domain.wishlist.controller;

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
            description = "이미 담긴 부품이면 기존 항목을 그대로 돌려준다"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/wishlist")
    public BaseResponse<WishlistResponse> add(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody WishlistAddRequest request
    ) {
        return BaseResponse.of(
                WishlistStatusCode.ADD_SUCCESS,
                wishlistService.add(Long.valueOf(memberId), request.partId())
        );
    }

    @Operation(
            summary = "부품 빼기",
            description = "id 는 위시리스트 항목 ID (부품 ID 아님)"
    )
    @DeleteMapping("/wishlist/{wishlistItemId}")
    public BaseResponse<WishlistResponse> remove(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @PathVariable Long wishlistItemId
    ) {
        return BaseResponse.of(
                WishlistStatusCode.REMOVE_SUCCESS,
                wishlistService.remove(Long.valueOf(memberId), wishlistItemId)
        );
    }

    @Operation(
            summary = "내 위시리스트",
            description = "최근 담은 순. cursor 는 이전 응답의 nextCursor"
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

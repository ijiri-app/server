package ijiri.ijiriserver.community.wishlist.adapter.`in`.web.controller

import ijiri.ijiriserver.community.wishlist.application.data.request.WishlistAddRequest
import ijiri.ijiriserver.community.wishlist.application.data.response.WishlistResponse
import ijiri.ijiriserver.community.wishlist.application.port.`in`.AddWishlistItemUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.GetWishlistUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.RemoveWishlistItemUseCase
import ijiri.ijiriserver.community.wishlist.exception.WishlistStatusCode
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Wishlist", description = "위시리스트")
@RestController
class WishlistController(
    private val addWishlistItemUseCase: AddWishlistItemUseCase,
    private val getWishlistUseCase: GetWishlistUseCase,
    private val removeWishlistItemUseCase: RemoveWishlistItemUseCase,
) {

    @Operation(
        summary = "부품 담기",
        description = "게시물 부품(postPartId)을 담는다. 새로 담으면 201, 이미 담겨 있으면 200 과 같은 id. " +
            "숨김·삭제된 게시물이면 NOT_FOUND",
    )
    @PostMapping("/wishlist")
    fun add(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: WishlistAddRequest,
    ): ResponseEntity<BaseResponse<WishlistResponse>> {
        val result = addWishlistItemUseCase.add(memberId.toLong(), request.postPartId!!)
        val statusCode = if (result.created) {
            WishlistStatusCode.ADD_SUCCESS
        } else {
            WishlistStatusCode.ALREADY_ADDED
        }
        return ResponseEntity.status(statusCode.httpStatus)
            .body(BaseResponse.of(statusCode, WishlistResponse.added(result.id)))
    }

    @Operation(
        summary = "부품 빼기",
        description = "id 는 위시리스트 항목 ID. 204",
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/wishlist/{wishlistItemId}")
    fun remove(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @PathVariable wishlistItemId: Long,
    ) {
        removeWishlistItemUseCase.remove(memberId.toLong(), wishlistItemId)
    }

    @Operation(
        summary = "내 위시리스트",
        description = "최근 담은 순. 원 게시물이 숨김·삭제되면 빠진다. cursor 는 이전 응답의 nextCursor",
    )
    @GetMapping("/members/me/wishlist")
    fun getWishlist(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): BaseResponse<WishlistResponse> =
        BaseResponse.ok(getWishlistUseCase.getWishlist(memberId.toLong(), cursor, size))
}

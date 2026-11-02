package ijiri.ijiriserver.domain.wishlist.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class WishlistAddRequest(
    @Schema(description = "게시물 상세의 parts[].postPartId", example = "11")
    @field:NotNull @field:Positive
    val postPartId: Long?,
)

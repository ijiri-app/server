package ijiri.ijiriserver.domain.wishlist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WishlistAddRequest(
        @Schema(description = "게시물 상세의 parts[].postPartId", example = "11")
        @NotNull @Positive Long postPartId
) {
}

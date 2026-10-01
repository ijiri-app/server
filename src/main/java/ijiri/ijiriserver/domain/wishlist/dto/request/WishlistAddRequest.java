package ijiri.ijiriserver.domain.wishlist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WishlistAddRequest(
        @Schema(description = "담을 부품 ID", example = "1")
        @NotNull @Positive Long partId
) {
}

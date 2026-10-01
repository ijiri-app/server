package ijiri.ijiriserver.domain.upload.dto.request;

import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UploadUrlRequest(
        @Schema(description = "올릴 사진 수 (1~10)", example = "3")
        @NotNull @Min(1) @Max(10) Integer count,

        @Schema(description = "image/jpeg, image/png, image/webp, image/heic 중 하나", example = "image/jpeg")
        @NotBlank @Pattern(regexp = "image/(jpeg|png|webp|heic)") String contentType,

        @Schema(description = "POST(게시물, 기본) / PROFILE(프로필 사진)", example = "POST")
        UploadPurpose purpose
) {

    public UploadUrlRequest {
        purpose = purpose == null ? UploadPurpose.POST : purpose;
    }
}

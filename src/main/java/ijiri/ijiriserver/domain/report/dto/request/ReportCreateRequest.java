package ijiri.ijiriserver.domain.report.dto.request;

import ijiri.ijiriserver.domain.report.entity.ReportReason;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @Schema(description = "신고 대상 종류", example = "POST")
        @NotNull ReportTargetType targetType,

        @Schema(description = "게시물 ID 또는 회원 ID", example = "1")
        @NotNull @Positive Long targetId,

        @Schema(description = "신고 사유", example = "SPAM")
        @NotNull ReportReason reason,

        @Schema(description = "상세 내용 (선택)")
        @Size(max = 500) String detail
) {
}

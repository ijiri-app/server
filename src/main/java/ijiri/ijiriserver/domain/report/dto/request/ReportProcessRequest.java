package ijiri.ijiriserver.domain.report.dto.request;

import ijiri.ijiriserver.domain.report.entity.ReportAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReportProcessRequest(
        @Schema(description = "HIDE_POST(게시물 신고만) / SUSPEND_MEMBER / DISMISS", example = "HIDE_POST")
        @NotNull ReportAction action,

        @Schema(description = "SUSPEND_MEMBER 일 때 정지 일수 (기본 7일)", example = "7")
        @Min(1) @Max(3650) Integer suspendDays
) {
}

package ijiri.ijiriserver.domain.report.application.data.request

import ijiri.ijiriserver.domain.report.entity.ReportAction
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ReportProcessRequest(
    @Schema(description = "HIDE_POST(게시물 신고만) / SUSPEND_MEMBER / DISMISS", example = "HIDE_POST")
    @field:NotNull
    val action: ReportAction?,

    @Schema(description = "SUSPEND_MEMBER 일 때 정지 일수 (기본 7일)", example = "7")
    @field:Min(1) @field:Max(3650)
    val suspendDays: Int?,
)

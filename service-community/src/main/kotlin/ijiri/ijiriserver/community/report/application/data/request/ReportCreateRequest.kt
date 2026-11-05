package ijiri.ijiriserver.community.report.application.data.request

import ijiri.ijiriserver.community.report.entity.ReportReason
import ijiri.ijiriserver.community.report.entity.ReportTargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class ReportCreateRequest(
    @Schema(description = "신고 대상 종류", example = "POST")
    @field:NotNull
    val targetType: ReportTargetType?,

    @Schema(description = "게시물 ID 또는 회원 ID", example = "1")
    @field:NotNull @field:Positive
    val targetId: Long?,

    @Schema(description = "신고 사유", example = "SPAM")
    @field:NotNull
    val reason: ReportReason?,

    @Schema(description = "상세 내용 (선택, 300자 이하)")
    @field:Size(max = 300)
    val detail: String?,
)

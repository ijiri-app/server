package ijiri.ijiriserver.domain.report.application.data

import ijiri.ijiriserver.domain.report.entity.ReportReason
import ijiri.ijiriserver.domain.report.entity.ReportTargetType
import java.time.LocalDateTime

/**
 * 관리자 신고 목록 한 줄. 같은 대상의 신고를 묶어 가장 먼저 들어온 신고의 id·사유와 신고 수를 보여준다.
 */
data class ReportSummary(
    val id: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val reportCount: Long,
    val createdAt: LocalDateTime,
)

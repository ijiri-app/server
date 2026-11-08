package ijiri.ijiriserver.community.report.application.port.out

import ijiri.ijiriserver.community.report.entity.Report
import ijiri.ijiriserver.community.report.entity.ReportStatus
import ijiri.ijiriserver.community.report.entity.ReportTargetType
import org.springframework.data.domain.Limit

interface ReportPort {

    fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean

    fun findFirstReportIdsByTarget(status: ReportStatus, cursor: Long, limit: Limit): List<Long>

    fun findAllByIdIn(ids: Collection<Long>): List<Report>

    fun countByTargetTypeAndTargetIdAndStatus(targetType: ReportTargetType, targetId: Long, status: ReportStatus): Long

    fun findAllByTargetTypeAndTargetIdAndStatus(
        targetType: ReportTargetType,
        targetId: Long,
        status: ReportStatus,
    ): List<Report>

    fun deleteAllRelatedTo(memberId: Long)

    fun findById(id: Long): Report?

    fun save(entity: Report): Report
}

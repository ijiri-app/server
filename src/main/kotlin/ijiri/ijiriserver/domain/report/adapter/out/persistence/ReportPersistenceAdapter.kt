package ijiri.ijiriserver.domain.report.adapter.out.persistence

import ijiri.ijiriserver.domain.report.application.port.out.ReportPort
import ijiri.ijiriserver.domain.report.entity.Report
import ijiri.ijiriserver.domain.report.entity.ReportStatus
import ijiri.ijiriserver.domain.report.entity.ReportTargetType
import org.springframework.data.domain.Limit
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ReportPersistenceAdapter(
    private val reportJpaRepository: ReportJpaRepository,
) : ReportPort {

    override fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean {
        return reportJpaRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)
    }

    override fun findFirstReportIdsByTarget(status: ReportStatus, cursor: Long, limit: Limit): List<Long> {
        return reportJpaRepository.findFirstReportIdsByTarget(status, cursor, limit)
    }

    override fun findAllByIdIn(ids: Collection<Long>): List<Report> {
        return reportJpaRepository.findAllByIdIn(ids)
    }

    override fun countByTargetTypeAndTargetIdAndStatus(
        targetType: ReportTargetType,
        targetId: Long,
        status: ReportStatus,
    ): Long {
        return reportJpaRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, status)
    }

    override fun findAllByTargetTypeAndTargetIdAndStatus(
        targetType: ReportTargetType,
        targetId: Long,
        status: ReportStatus,
    ): List<Report> {
        return reportJpaRepository.findAllByTargetTypeAndTargetIdAndStatus(targetType, targetId, status)
    }

    override fun deleteAllRelatedTo(memberId: Long) {
        reportJpaRepository.deleteAllRelatedTo(memberId)
    }

    override fun findById(id: Long): Report? {
        return reportJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: Report): Report {
        return reportJpaRepository.save(entity)
    }
}

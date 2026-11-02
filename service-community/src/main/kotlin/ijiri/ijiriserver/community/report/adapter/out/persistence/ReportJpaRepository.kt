package ijiri.ijiriserver.community.report.adapter.out.persistence

import ijiri.ijiriserver.community.report.entity.Report
import ijiri.ijiriserver.community.report.entity.ReportStatus
import ijiri.ijiriserver.community.report.entity.ReportTargetType
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReportJpaRepository : JpaRepository<Report, Long> {

    fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean

    // 대상별로 묶어 가장 먼저 들어온 신고 id 의 역순(최근 대상부터)
    @Query(
        """
        SELECT MIN(r.id) FROM Report r
        WHERE r.status = :status
        GROUP BY r.targetType, r.targetId
        HAVING MIN(r.id) < :cursor
        ORDER BY MIN(r.id) DESC
        """
    )
    fun findFirstReportIdsByTarget(
        @Param("status") status: ReportStatus,
        @Param("cursor") cursor: Long,
        limit: Limit,
    ): List<Long>

    fun findAllByIdIn(ids: Collection<Long>): List<Report>

    fun countByTargetTypeAndTargetIdAndStatus(targetType: ReportTargetType, targetId: Long, status: ReportStatus): Long

    fun findAllByTargetTypeAndTargetIdAndStatus(
        targetType: ReportTargetType,
        targetId: Long,
        status: ReportStatus,
    ): List<Report>

    @Modifying
    @Query("DELETE FROM Report r WHERE r.reporterId = :memberId OR r.targetMemberId = :memberId")
    fun deleteAllRelatedTo(@Param("memberId") memberId: Long)
}

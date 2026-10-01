package ijiri.ijiriserver.domain.report.repository;

import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 대상별로 묶어 가장 먼저 들어온 신고 id 의 역순(최근 대상부터)
    @Query("""
            SELECT MIN(r.id) FROM Report r
            WHERE r.status = :status
            GROUP BY r.targetType, r.targetId
            HAVING MIN(r.id) < :cursor
            ORDER BY MIN(r.id) DESC
            """)
    List<Long> findFirstReportIdsByTarget(
            @Param("status") ReportStatus status,
            @Param("cursor") Long cursor,
            Limit limit
    );

    List<Report> findAllByIdIn(Collection<Long> ids);

    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);

    List<Report> findAllByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    );

    @Modifying
    @Query("DELETE FROM Report r WHERE r.reporterId = :memberId OR r.targetMemberId = :memberId")
    void deleteAllRelatedTo(@Param("memberId") Long memberId);
}

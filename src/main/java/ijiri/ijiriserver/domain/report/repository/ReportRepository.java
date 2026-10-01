package ijiri.ijiriserver.domain.report.repository;

import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    List<Report> findByStatusAndIdLessThanOrderByIdDesc(ReportStatus status, Long cursor, Limit limit);

    List<Report> findAllByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    );

    @Modifying
    @Query("DELETE FROM Report r WHERE r.reporterId = :memberId OR r.targetMemberId = :memberId")
    void deleteAllRelatedTo(@Param("memberId") Long memberId);
}

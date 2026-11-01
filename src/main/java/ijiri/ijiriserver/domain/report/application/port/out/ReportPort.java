package ijiri.ijiriserver.domain.report.application.port.out;

import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Limit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportPort {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    List<Long> findFirstReportIdsByTarget(ReportStatus status, Long cursor, Limit limit);

    List<Report> findAllByIdIn(Collection<Long> ids);

    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);

    List<Report> findAllByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    );

    void deleteAllRelatedTo(Long memberId);

    Optional<Report> findById(Long id);

    Report save(Report entity);
}

package ijiri.ijiriserver.domain.report.adapter.out.persistence;

import ijiri.ijiriserver.domain.report.application.port.out.ReportPort;
import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements ReportPort {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    ) {
        return reportJpaRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId);
    }

    @Override
    public List<Long> findFirstReportIdsByTarget(ReportStatus status, Long cursor, Limit limit) {
        return reportJpaRepository.findFirstReportIdsByTarget(status, cursor, limit);
    }

    @Override
    public List<Report> findAllByIdIn(Collection<Long> ids) {
        return reportJpaRepository.findAllByIdIn(ids);
    }

    @Override
    public long countByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    ) {
        return reportJpaRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, status);
    }

    @Override
    public List<Report> findAllByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    ) {
        return reportJpaRepository.findAllByTargetTypeAndTargetIdAndStatus(targetType, targetId, status);
    }

    @Override
    public void deleteAllRelatedTo(Long memberId) {
        reportJpaRepository.deleteAllRelatedTo(memberId);
    }

    @Override
    public Optional<Report> findById(Long id) {
        return reportJpaRepository.findById(id);
    }

    @Override
    public Report save(Report entity) {
        return reportJpaRepository.save(entity);
    }
}

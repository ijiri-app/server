package ijiri.ijiriserver.domain.report.service.impl;

import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.post.service.PostService;
import ijiri.ijiriserver.domain.report.dto.request.ReportCreateRequest;
import ijiri.ijiriserver.domain.report.dto.request.ReportProcessRequest;
import ijiri.ijiriserver.domain.report.dto.response.ReportResponse;
import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportAction;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;
import ijiri.ijiriserver.domain.report.exception.ReportStatusCode;
import ijiri.ijiriserver.domain.report.repository.ReportRepository;
import ijiri.ijiriserver.domain.report.service.ReportService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import ijiri.ijiriserver.domain.report.repository.ReportSummary;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_SUSPEND_DAYS = 7;

    private final ReportRepository reportRepository;
    private final PostService postService;
    private final MemberService memberService;
    private final Clock clock;

    @Override
    @Transactional
    public void report(Long reporterId, ReportCreateRequest request) {
        memberService.getById(reporterId);
        Long targetMemberId = resolveTargetMemberId(request.targetType(), request.targetId());
        if (targetMemberId.equals(reporterId)) {
            throw new CustomException(ReportStatusCode.CANNOT_REPORT_SELF);
        }
        // 같은 사람이 같은 대상을 다시 신고하면 한 건으로 본다
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId,
                request.targetType(),
                request.targetId()
        )) {
            return;
        }
        reportRepository.save(Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .targetMemberId(targetMemberId)
                .reason(request.reason())
                .detail(request.detail())
                .status(ReportStatus.PENDING)
                .build()
        );
    }

    @Override
    public ReportResponse getReports(ReportStatus status, Long cursor, int size) {
        List<Long> found = reportRepository.findFirstReportIdsByTarget(
                status,
                cursor != null ? cursor : Long.MAX_VALUE,
                Limit.of(size + 1)
        );
        boolean hasNext = found.size() > size;
        List<Long> page = hasNext ? found.subList(0, size) : found;
        Map<Long, Report> reports = reportRepository.findAllByIdIn(page).stream()
                .collect(Collectors.toMap(Report::getId, Function.identity()));
        List<ReportSummary> items = page.stream()
                .map(reports::get)
                .map(report -> new ReportSummary(
                        report.getId(),
                        report.getTargetType(),
                        report.getTargetId(),
                        report.getReason(),
                        reportRepository.countByTargetTypeAndTargetIdAndStatus(
                                report.getTargetType(),
                                report.getTargetId(),
                                status
                        ),
                        report.getCreatedAt()
                ))
                .toList();
        return ReportResponse.list(items, hasNext ? page.getLast() : null);
    }

    // 같은 대상에 쌓인 대기 중 신고를 한 번에 같은 결과로 처리한다
    @Override
    @Transactional
    public void process(Long adminId, Long reportId, ReportProcessRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ReportStatusCode.REPORT_NOT_FOUND));
        if (!report.isPending()) {
            throw new CustomException(ReportStatusCode.ALREADY_PROCESSED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        switch (request.action()) {
            case HIDE_POST -> {
                if (report.getTargetType() != ReportTargetType.POST) {
                    throw new CustomException(ReportStatusCode.INVALID_ACTION);
                }
                postService.hide(report.getTargetId());
            }
            case SUSPEND_MEMBER -> {
                int days = request.suspendDays() != null ? request.suspendDays() : DEFAULT_SUSPEND_DAYS;
                memberService.suspend(report.getTargetMemberId(), now.plusDays(days));
            }
            case DISMISS -> {
                // 조치 없이 신고만 닫는다
            }
        }
        resolvePending(report, request.action(), adminId, now);
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        reportRepository.deleteAllRelatedTo(event.memberId());
    }

    // 처리 중인 신고 자신도 대기 상태이므로 함께 조회된다
    private void resolvePending(Report report, ReportAction action, Long adminId, LocalDateTime now) {
        reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                report.getTargetType(),
                report.getTargetId(),
                ReportStatus.PENDING
        ).forEach(pending -> pending.resolve(action, adminId, now));
    }

    private Long resolveTargetMemberId(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postService.getAuthorId(targetId);
            case MEMBER -> memberService.getById(targetId).getId();
        };
    }
}

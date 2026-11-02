package ijiri.ijiriserver.domain.report.application.service

import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.SuspendMemberUseCase
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.domain.post.application.port.`in`.GetPostAuthorIdUseCase
import ijiri.ijiriserver.domain.post.application.port.`in`.HidePostUseCase
import ijiri.ijiriserver.domain.report.application.data.ReportSummary
import ijiri.ijiriserver.domain.report.application.data.request.ReportCreateRequest
import ijiri.ijiriserver.domain.report.application.data.request.ReportProcessRequest
import ijiri.ijiriserver.domain.report.application.data.response.ReportResponse
import ijiri.ijiriserver.domain.report.application.port.`in`.CreateReportUseCase
import ijiri.ijiriserver.domain.report.application.port.`in`.GetReportsUseCase
import ijiri.ijiriserver.domain.report.application.port.`in`.ProcessReportUseCase
import ijiri.ijiriserver.domain.report.application.port.out.ReportPort
import ijiri.ijiriserver.domain.report.entity.Report
import ijiri.ijiriserver.domain.report.entity.ReportAction
import ijiri.ijiriserver.domain.report.entity.ReportStatus
import ijiri.ijiriserver.domain.report.entity.ReportTargetType
import ijiri.ijiriserver.domain.report.exception.ReportStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReportService(
    private val reportPort: ReportPort,
    private val getPostAuthorIdUseCase: GetPostAuthorIdUseCase,
    private val hidePostUseCase: HidePostUseCase,
    private val getMemberUseCase: GetMemberUseCase,
    private val suspendMemberUseCase: SuspendMemberUseCase,
    private val clock: Clock,
) : CreateReportUseCase,
    GetReportsUseCase,
    ProcessReportUseCase {

    @Transactional
    override fun report(reporterId: Long, request: ReportCreateRequest) {
        val targetType = request.targetType!!
        val targetId = request.targetId!!
        getMemberUseCase.getById(reporterId)
        val targetMemberId = resolveTargetMemberId(targetType, targetId)
        if (targetMemberId == reporterId) {
            throw CustomException(ReportStatusCode.CANNOT_REPORT_SELF)
        }
        // 같은 사람이 같은 대상을 다시 신고하면 한 건으로 본다
        if (reportPort.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            return
        }
        reportPort.save(
            Report(
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                targetMemberId = targetMemberId,
                reason = request.reason!!,
                detail = request.detail,
                status = ReportStatus.PENDING,
            )
        )
    }

    override fun getReports(status: ReportStatus, cursor: Long?, size: Int): ReportResponse {
        val found = reportPort.findFirstReportIdsByTarget(
            status,
            cursor ?: Long.MAX_VALUE,
            Limit.of(size + 1),
        )
        val hasNext = found.size > size
        val page = if (hasNext) found.subList(0, size) else found
        val reports = reportPort.findAllByIdIn(page).associateBy { it.id }
        val items = page
            .map { reports.getValue(it) }
            .map { report ->
                ReportSummary(
                    report.id,
                    report.targetType,
                    report.targetId,
                    report.reason,
                    reportPort.countByTargetTypeAndTargetIdAndStatus(
                        report.targetType,
                        report.targetId,
                        status,
                    ),
                    report.createdAt,
                )
            }
        return ReportResponse.list(items, if (hasNext) page.last() else null)
    }

    // 같은 대상에 쌓인 대기 중 신고를 한 번에 같은 결과로 처리한다
    @Transactional
    override fun process(adminId: Long, reportId: Long, request: ReportProcessRequest) {
        val action = request.action!!
        val report = reportPort.findById(reportId) ?: throw CustomException(ReportStatusCode.REPORT_NOT_FOUND)
        if (!report.isPending()) {
            throw CustomException(ReportStatusCode.ALREADY_PROCESSED)
        }
        val now = LocalDateTime.now(clock)
        when (action) {
            ReportAction.HIDE_POST -> {
                if (report.targetType != ReportTargetType.POST) {
                    throw CustomException(ReportStatusCode.INVALID_ACTION)
                }
                hidePostUseCase.hide(report.targetId)
            }
            ReportAction.SUSPEND_MEMBER -> {
                val days = request.suspendDays ?: DEFAULT_SUSPEND_DAYS
                suspendMemberUseCase.suspend(report.targetMemberId, now.plusDays(days.toLong()))
            }
            ReportAction.DISMISS -> {
                // 조치 없이 신고만 닫는다
            }
        }
        resolvePending(report, action, adminId, now)
    }

    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        reportPort.deleteAllRelatedTo(event.memberId)
    }

    // 처리 중인 신고 자신도 대기 상태이므로 함께 조회된다
    private fun resolvePending(report: Report, action: ReportAction, adminId: Long, now: LocalDateTime) {
        reportPort.findAllByTargetTypeAndTargetIdAndStatus(
            report.targetType,
            report.targetId,
            ReportStatus.PENDING,
        ).forEach { it.resolve(action, adminId, now) }
    }

    private fun resolveTargetMemberId(targetType: ReportTargetType, targetId: Long): Long {
        return when (targetType) {
            ReportTargetType.POST -> getPostAuthorIdUseCase.getAuthorId(targetId)
            ReportTargetType.MEMBER -> getMemberUseCase.getById(targetId).id
        }
    }

    companion object {
        private const val DEFAULT_SUSPEND_DAYS = 7
    }
}

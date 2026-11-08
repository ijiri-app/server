package ijiri.ijiriserver.community.report.application.service

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.community.post.application.port.`in`.GetPostAuthorIdUseCase
import ijiri.ijiriserver.community.post.application.port.`in`.HidePostUseCase
import ijiri.ijiriserver.community.post.application.port.out.MemberReader
import ijiri.ijiriserver.community.report.application.data.ReportSummary
import ijiri.ijiriserver.community.report.application.data.request.ReportCreateRequest
import ijiri.ijiriserver.community.report.application.data.request.ReportProcessRequest
import ijiri.ijiriserver.community.report.application.data.response.ReportResponse
import ijiri.ijiriserver.community.report.application.port.`in`.CreateReportUseCase
import ijiri.ijiriserver.community.report.application.port.`in`.DeleteMemberReportsUseCase
import ijiri.ijiriserver.community.report.application.port.`in`.GetReportsUseCase
import ijiri.ijiriserver.community.report.application.port.`in`.ProcessReportUseCase
import ijiri.ijiriserver.community.report.application.port.out.MemberSuspender
import ijiri.ijiriserver.community.report.application.port.out.ReportPort
import ijiri.ijiriserver.community.report.entity.Report
import ijiri.ijiriserver.community.report.entity.ReportAction
import ijiri.ijiriserver.community.report.entity.ReportStatus
import ijiri.ijiriserver.community.report.entity.ReportTargetType
import ijiri.ijiriserver.community.report.exception.ReportStatusCode
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
    private val memberReader: MemberReader,
    private val memberSuspender: MemberSuspender,
    private val clock: Clock,
) : CreateReportUseCase,
    GetReportsUseCase,
    ProcessReportUseCase,
    DeleteMemberReportsUseCase {

    // 신고하는 쪽은 게이트웨이가 활성 회원임을 확인했다
    @Transactional
    override fun report(reporterId: Long, request: ReportCreateRequest) {
        val targetType = request.targetType!!
        val targetId = request.targetId!!
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

    // 같은 대상에 쌓인 대기 중 신고를 한 번에 같은 결과로 처리한다.
    // 이용 정지는 service-user 가 바로 적용하므로(세션도 끊는다), 그 뒤 이 트랜잭션이 실패하면 신고는 다시 처리하면 된다
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
                memberSuspender.suspend(report.targetMemberId, now.plusDays(days.toLong()))
            }
            ReportAction.DISMISS -> {
                // 조치 없이 신고만 닫는다
            }
        }
        resolvePending(report, action, adminId, now)
    }

    @Transactional
    override fun deleteAll(memberId: Long) {
        reportPort.deleteAllRelatedTo(memberId)
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
            ReportTargetType.MEMBER -> memberReader.findActive(targetId)?.id
                ?: throw CustomException(ReportStatusCode.MEMBER_NOT_FOUND)
        }
    }

    companion object {
        private const val DEFAULT_SUSPEND_DAYS = 7
    }
}

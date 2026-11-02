package ijiri.ijiriserver.domain.report.entity

import ijiri.ijiriserver.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 게시물·사용자 신고. 같은 회원이 같은 대상을 다시 신고하면 한 건으로 본다.
 * 처리 기록(누가 resolvedBy, 언제 resolvedAt, 무엇을 action)을 남긴다.
 * targetMemberId 는 신고된 사용자(게시물이면 작성자)로, 정지 처리와 영구 삭제 정리에 쓴다.
 */
@Entity
@Table(
    name = "report",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_report_reporter_id_target_type_target_id",
            columnNames = ["reporter_id", "target_type", "target_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_report_status_target_type_target_id",
            columnList = "status, target_type, target_id",
        ),
        Index(name = "idx_report_target_member_id", columnList = "target_member_id"),
    ],
)
class Report(
    reporterId: Long,
    targetType: ReportTargetType,
    targetId: Long,
    targetMemberId: Long,
    reason: ReportReason,
    detail: String? = null,
    status: ReportStatus,
    action: ReportAction? = null,
    resolvedBy: Long? = null,
    resolvedAt: LocalDateTime? = null,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "reporter_id", nullable = false, updatable = false)
    var reporterId: Long = reporterId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20, updatable = false)
    var targetType: ReportTargetType = targetType
        protected set

    @Column(name = "target_id", nullable = false, updatable = false)
    var targetId: Long = targetId
        protected set

    @Column(name = "target_member_id", nullable = false, updatable = false)
    var targetMemberId: Long = targetMemberId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30, updatable = false)
    var reason: ReportReason = reason
        protected set

    @Column(name = "detail", length = 300, updatable = false)
    var detail: String? = detail
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = status
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    var action: ReportAction? = action
        protected set

    @Column(name = "resolved_by")
    var resolvedBy: Long? = resolvedBy
        protected set

    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = resolvedAt
        protected set

    fun isPending(): Boolean {
        return status == ReportStatus.PENDING
    }

    fun resolve(action: ReportAction, adminId: Long, now: LocalDateTime) {
        this.status = if (action == ReportAction.DISMISS) ReportStatus.DISMISSED else ReportStatus.ACTIONED
        this.action = action
        this.resolvedBy = adminId
        this.resolvedAt = now
    }
}

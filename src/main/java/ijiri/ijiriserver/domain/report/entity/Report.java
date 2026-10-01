package ijiri.ijiriserver.domain.report.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시물·사용자 신고. 한 회원은 같은 대상을 한 번만 신고할 수 있다.
 * targetMemberId 는 신고된 사용자(게시물이면 작성자)로, 정지 처리와 영구 삭제 정리에 쓴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "report",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_reporter_id_target_type_target_id",
                columnNames = {"reporter_id", "target_type", "target_id"}
        ),
        indexes = {
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_target_member_id", columnList = "target_member_id")
        }
)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20, updatable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private Long targetId;

    @Column(name = "target_member_id", nullable = false, updatable = false)
    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30, updatable = false)
    private ReportReason reason;

    @Column(name = "detail", length = 500, updatable = false)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private ReportAction action;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    public void resolve(ReportAction action, Long adminId, LocalDateTime now) {
        this.status = action == ReportAction.DISMISS ? ReportStatus.DISMISSED : ReportStatus.ACTIONED;
        this.action = action;
        this.resolvedBy = adminId;
        this.resolvedAt = now;
    }
}

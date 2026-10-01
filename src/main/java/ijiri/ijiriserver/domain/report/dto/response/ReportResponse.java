package ijiri.ijiriserver.domain.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.report.entity.Report;
import ijiri.ijiriserver.domain.report.entity.ReportAction;
import ijiri.ijiriserver.domain.report.entity.ReportReason;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * report 도메인의 모든 API 응답. 관리자 목록은 reports(+nextCursor), 처리는 report, 신고 접수는 message 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportResponse(
        List<Item> reports,
        Long nextCursor,
        Item report,
        String message
) {

    public static ReportResponse list(List<Report> reports, Long nextCursor) {
        return new ReportResponse(reports.stream().map(Item::from).toList(), nextCursor, null, null);
    }

    public static ReportResponse single(Report report) {
        return new ReportResponse(null, null, Item.from(report), null);
    }

    public static ReportResponse message(String message) {
        return new ReportResponse(null, null, null, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            Long id,
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            Long targetMemberId,
            ReportReason reason,
            String detail,
            ReportStatus status,
            ReportAction action,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt
    ) {

        static Item from(Report report) {
            return new Item(
                    report.getId(),
                    report.getReporterId(),
                    report.getTargetType(),
                    report.getTargetId(),
                    report.getTargetMemberId(),
                    report.getReason(),
                    report.getDetail(),
                    report.getStatus(),
                    report.getAction(),
                    report.getCreatedAt(),
                    report.getResolvedAt()
            );
        }
    }
}

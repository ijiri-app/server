package ijiri.ijiriserver.domain.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.report.repository.ReportSummary;

import java.util.List;

/**
 * report 도메인의 모든 API 응답 (관리자 신고 목록). 신고 접수·처리는 본문이 없다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportResponse(
        List<ReportSummary> items,
        String nextCursor
) {

    public static ReportResponse list(List<ReportSummary> items, Long nextCursor) {
        return new ReportResponse(items, nextCursor != null ? String.valueOf(nextCursor) : null);
    }
}

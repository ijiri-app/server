package ijiri.ijiriserver.domain.report.application.data.response

import com.fasterxml.jackson.annotation.JsonInclude
import ijiri.ijiriserver.domain.report.application.data.ReportSummary

/**
 * report 도메인의 모든 API 응답 (관리자 신고 목록). 신고 접수·처리는 본문이 없다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportResponse(
    val items: List<ReportSummary>,
    val nextCursor: String?,
) {

    companion object {
        fun list(items: List<ReportSummary>, nextCursor: Long?): ReportResponse {
            return ReportResponse(items, nextCursor?.toString())
        }
    }
}

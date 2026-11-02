package ijiri.ijiriserver.domain.report.application.port.`in`

import ijiri.ijiriserver.domain.report.application.data.response.ReportResponse
import ijiri.ijiriserver.domain.report.entity.ReportStatus

interface GetReportsUseCase {

    fun getReports(status: ReportStatus, cursor: Long?, size: Int): ReportResponse
}

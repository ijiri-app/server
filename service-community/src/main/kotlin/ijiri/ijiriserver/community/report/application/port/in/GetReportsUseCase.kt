package ijiri.ijiriserver.community.report.application.port.`in`

import ijiri.ijiriserver.community.report.application.data.response.ReportResponse
import ijiri.ijiriserver.community.report.entity.ReportStatus

interface GetReportsUseCase {

    fun getReports(status: ReportStatus, cursor: Long?, size: Int): ReportResponse
}

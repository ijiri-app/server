package ijiri.ijiriserver.community.report.application.port.`in`

import ijiri.ijiriserver.community.report.application.data.request.ReportCreateRequest

interface CreateReportUseCase {

    fun report(reporterId: Long, request: ReportCreateRequest)
}

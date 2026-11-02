package ijiri.ijiriserver.domain.report.application.port.`in`

import ijiri.ijiriserver.domain.report.application.data.request.ReportCreateRequest

interface CreateReportUseCase {

    fun report(reporterId: Long, request: ReportCreateRequest)
}

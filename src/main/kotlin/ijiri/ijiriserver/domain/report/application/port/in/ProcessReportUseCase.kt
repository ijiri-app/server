package ijiri.ijiriserver.domain.report.application.port.`in`

import ijiri.ijiriserver.domain.report.application.data.request.ReportProcessRequest

interface ProcessReportUseCase {

    fun process(adminId: Long, reportId: Long, request: ReportProcessRequest)
}

package ijiri.ijiriserver.community.report.application.port.`in`

import ijiri.ijiriserver.community.report.application.data.request.ReportProcessRequest

interface ProcessReportUseCase {

    fun process(adminId: Long, reportId: Long, request: ReportProcessRequest)
}

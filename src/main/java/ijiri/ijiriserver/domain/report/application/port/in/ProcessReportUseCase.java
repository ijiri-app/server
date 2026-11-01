package ijiri.ijiriserver.domain.report.application.port.in;

import ijiri.ijiriserver.domain.report.application.data.request.ReportProcessRequest;

public interface ProcessReportUseCase {

    void process(Long adminId, Long reportId, ReportProcessRequest request);
}

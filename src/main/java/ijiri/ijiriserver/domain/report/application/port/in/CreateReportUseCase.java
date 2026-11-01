package ijiri.ijiriserver.domain.report.application.port.in;

import ijiri.ijiriserver.domain.report.application.data.request.ReportCreateRequest;

public interface CreateReportUseCase {

    void report(Long reporterId, ReportCreateRequest request);
}

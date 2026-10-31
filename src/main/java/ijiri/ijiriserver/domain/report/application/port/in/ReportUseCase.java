package ijiri.ijiriserver.domain.report.application.port.in;

import ijiri.ijiriserver.domain.report.application.data.request.ReportCreateRequest;
import ijiri.ijiriserver.domain.report.application.data.request.ReportProcessRequest;
import ijiri.ijiriserver.domain.report.application.data.response.ReportResponse;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;

public interface ReportUseCase {

    void report(Long reporterId, ReportCreateRequest request);

    ReportResponse getReports(ReportStatus status, Long cursor, int size);

    void process(Long adminId, Long reportId, ReportProcessRequest request);
}

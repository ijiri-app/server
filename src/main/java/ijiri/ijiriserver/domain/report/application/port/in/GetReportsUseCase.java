package ijiri.ijiriserver.domain.report.application.port.in;

import ijiri.ijiriserver.domain.report.application.data.response.ReportResponse;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;

public interface GetReportsUseCase {

    ReportResponse getReports(ReportStatus status, Long cursor, int size);
}

package ijiri.ijiriserver.domain.report.service;

import ijiri.ijiriserver.domain.report.dto.request.ReportCreateRequest;
import ijiri.ijiriserver.domain.report.dto.request.ReportProcessRequest;
import ijiri.ijiriserver.domain.report.dto.response.ReportResponse;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;

public interface ReportService {

    ReportResponse report(Long reporterId, ReportCreateRequest request);

    ReportResponse getReports(ReportStatus status, Long cursor, int size);

    ReportResponse process(Long adminId, Long reportId, ReportProcessRequest request);
}

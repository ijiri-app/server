package ijiri.ijiriserver.domain.report.controller;

import ijiri.ijiriserver.domain.report.dto.request.ReportCreateRequest;
import ijiri.ijiriserver.domain.report.dto.response.ReportResponse;
import ijiri.ijiriserver.domain.report.exception.ReportStatusCode;
import ijiri.ijiriserver.domain.report.service.ReportService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "신고")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "게시물·사용자 신고",
            description = "같은 대상은 한 번만 신고할 수 있다(REPORT409). 관리자가 확인 후 숨김·정지 처리"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BaseResponse<ReportResponse> report(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return BaseResponse.of(ReportStatusCode.REPORT_SUCCESS, reportService.report(Long.valueOf(memberId), request));
    }
}

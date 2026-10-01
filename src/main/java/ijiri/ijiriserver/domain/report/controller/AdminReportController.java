package ijiri.ijiriserver.domain.report.controller;

import ijiri.ijiriserver.domain.report.dto.request.ReportProcessRequest;
import ijiri.ijiriserver.domain.report.dto.response.ReportResponse;
import ijiri.ijiriserver.domain.report.entity.ReportStatus;
import ijiri.ijiriserver.domain.report.exception.ReportStatusCode;
import ijiri.ijiriserver.domain.report.service.ReportService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN 역할만 호출할 수 있다 (SecurityConfig 의 /admin/**).
 */
@Tag(name = "Admin", description = "관리자 신고 처리")
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 목록",
            description = "상태별 최신순. cursor 는 이전 응답의 nextCursor"
    )
    @GetMapping
    public BaseResponse<ReportResponse> getReports(
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return BaseResponse.ok(reportService.getReports(status, cursor, size));
    }

    @Operation(
            summary = "신고 처리",
            description = "게시물 숨김(HIDE_POST), 사용자 정지(SUSPEND_MEMBER, 세션 즉시 종료), 기각(DISMISS). "
                    + "같은 대상의 대기 중 신고도 함께 처리된다"
    )
    @PatchMapping("/{reportId}")
    public BaseResponse<ReportResponse> process(
            @Parameter(hidden = true) @AuthenticationPrincipal String adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request
    ) {
        return BaseResponse.of(
                ReportStatusCode.PROCESS_SUCCESS,
                reportService.process(Long.valueOf(adminId), reportId, request)
        );
    }
}

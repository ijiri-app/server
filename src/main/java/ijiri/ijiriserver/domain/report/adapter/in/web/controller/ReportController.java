package ijiri.ijiriserver.domain.report.adapter.in.web.controller;

import ijiri.ijiriserver.domain.report.application.data.request.ReportCreateRequest;
import ijiri.ijiriserver.domain.report.application.port.in.CreateReportUseCase;
import ijiri.ijiriserver.domain.report.entity.Report;
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

    private final CreateReportUseCase createReportUseCase;

    @Operation(
            summary = "게시물·사용자 신고",
            description = "사유: SPAM, INAPPROPRIATE, ILLEGAL_TUNING(공도 레이싱·불법 튜닝 홍보), COPYRIGHT, OTHER. "
                    + "같은 대상을 다시 신고하면 한 건으로 본다. 관리자가 확인 후 숨김·정지 처리. 201"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void report(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        createReportUseCase.report(Long.valueOf(memberId), request);
    }
}

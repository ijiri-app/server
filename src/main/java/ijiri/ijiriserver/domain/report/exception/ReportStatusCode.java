package ijiri.ijiriserver.domain.report.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportStatusCode implements StatusCode {

    REPORT_SUCCESS(HttpStatus.CREATED, "REPORT201", "신고가 접수되었습니다."),
    PROCESS_SUCCESS(HttpStatus.OK, "REPORT2001", "신고가 처리되었습니다."),
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT4001", "자신이나 자신의 게시물은 신고할 수 없습니다."),
    INVALID_ACTION(HttpStatus.BAD_REQUEST, "REPORT4002", "게시물 숨김은 게시물 신고에만 할 수 있습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404", "신고를 찾을 수 없습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "REPORT409", "이미 신고한 대상입니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "REPORT4091", "이미 처리된 신고입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

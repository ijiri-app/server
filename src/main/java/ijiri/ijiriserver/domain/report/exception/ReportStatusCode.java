package ijiri.ijiriserver.domain.report.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportStatusCode implements StatusCode {

    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "CANNOT_REPORT_SELF", "자신이나 자신의 게시물은 신고할 수 없습니다."),
    INVALID_ACTION(HttpStatus.BAD_REQUEST, "INVALID_ACTION", "게시물 숨김은 게시물 신고에만 할 수 있습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "신고를 찾을 수 없습니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "ALREADY_PROCESSED", "이미 처리된 신고입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

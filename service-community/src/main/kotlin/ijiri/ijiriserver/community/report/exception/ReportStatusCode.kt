package ijiri.ijiriserver.community.report.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class ReportStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "CANNOT_REPORT_SELF", "자신이나 자신의 게시물은 신고할 수 없습니다."),
    INVALID_ACTION(HttpStatus.BAD_REQUEST, "INVALID_ACTION", "게시물 숨김은 게시물 신고에만 할 수 있습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "신고를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "회원을 찾을 수 없습니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "ALREADY_PROCESSED", "이미 처리된 신고입니다."),
}

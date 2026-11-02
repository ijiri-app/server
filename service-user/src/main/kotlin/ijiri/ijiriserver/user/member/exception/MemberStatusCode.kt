package ijiri.ijiriserver.user.member.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class MemberStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    UPDATE_SUCCESS(HttpStatus.OK, "MEMBER_UPDATED", "회원 정보가 수정되었습니다."),
    MEMBER_WITHDRAWN(
        HttpStatus.FORBIDDEN,
        "MEMBER_WITHDRAWN",
        "탈퇴한 계정입니다. 탈퇴 후 30일이 지나야 같은 계정으로 다시 가입할 수 있습니다.",
    ),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "MEMBER_SUSPENDED", "이용이 정지된 계정입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다."),
}

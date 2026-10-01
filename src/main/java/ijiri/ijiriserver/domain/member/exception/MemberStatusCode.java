package ijiri.ijiriserver.domain.member.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberStatusCode implements StatusCode {

    MEMBER_WITHDRAWN(
            HttpStatus.FORBIDDEN,
            "MEMBER403",
            "탈퇴한 계정입니다. 탈퇴 후 30일이 지나야 같은 계정으로 다시 가입할 수 있습니다."
    ),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "MEMBER4031", "이용이 정지된 계정입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER409", "이미 가입된 이메일입니다."),
    WITHDRAW_SUCCESS(HttpStatus.OK, "MEMBER2001", "회원 탈퇴가 완료되었습니다."),
    UPDATE_SUCCESS(HttpStatus.OK, "MEMBER2002", "회원 정보가 수정되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

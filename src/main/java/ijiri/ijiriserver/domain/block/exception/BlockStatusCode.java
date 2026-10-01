package ijiri.ijiriserver.domain.block.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockStatusCode implements StatusCode {

    BLOCK_SUCCESS(HttpStatus.OK, "BLOCK200", "사용자를 차단했습니다."),
    UNBLOCK_SUCCESS(HttpStatus.OK, "BLOCK2001", "차단을 해제했습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "BLOCK4001", "자기 자신은 차단할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

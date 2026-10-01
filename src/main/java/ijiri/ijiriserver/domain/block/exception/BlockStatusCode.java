package ijiri.ijiriserver.domain.block.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BlockStatusCode implements StatusCode {

    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "CANNOT_BLOCK_SELF", "자기 자신은 차단할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

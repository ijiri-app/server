package ijiri.ijiriserver.domain.ownedcar.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OwnedCarStatusCode implements StatusCode {

    CREATE_SUCCESS(HttpStatus.CREATED, "OWNED_CAR_CREATED", "보유 차량이 등록되었습니다."),
    OWNED_CAR_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "OWNED_CAR_LIMIT_EXCEEDED", "지금 타는 차는 최대 10대까지 등록할 수 있습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 차량만 사용할 수 있습니다."),
    OWNED_CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "보유 차량을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

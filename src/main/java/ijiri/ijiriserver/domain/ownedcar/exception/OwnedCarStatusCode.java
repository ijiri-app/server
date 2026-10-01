package ijiri.ijiriserver.domain.ownedcar.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OwnedCarStatusCode implements StatusCode {

    CREATE_SUCCESS(HttpStatus.CREATED, "OWNEDCAR201", "보유 차량이 등록되었습니다."),
    UPDATE_SUCCESS(HttpStatus.OK, "OWNEDCAR2001", "보유 차량이 수정되었습니다."),
    DELETE_SUCCESS(HttpStatus.OK, "OWNEDCAR2002", "보유 차량이 삭제되었습니다."),
    OWNED_CAR_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "OWNEDCAR4001", "보유 차량은 최대 10대까지 등록할 수 있습니다."),
    OWNED_CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "OWNEDCAR404", "보유 차량을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package ijiri.ijiriserver.domain.carmodel.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CarModelStatusCode implements StatusCode {

    INVALID_CAR_SPEC(HttpStatus.BAD_REQUEST, "CARMODEL4001", "차종·세대·트림 조합이 올바르지 않습니다."),
    CAR_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "CARMODEL404", "차종을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

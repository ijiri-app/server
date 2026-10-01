package ijiri.ijiriserver.domain.carmodel.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CarModelStatusCode implements StatusCode {

    INVALID_TRIM(HttpStatus.BAD_REQUEST, "INVALID_TRIM", "트림을 찾을 수 없습니다."),
    INVALID_MODEL_YEAR(HttpStatus.BAD_REQUEST, "INVALID_MODEL_YEAR", "연식이 세대의 판매 기간을 벗어났습니다."),
    CAR_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "차종을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

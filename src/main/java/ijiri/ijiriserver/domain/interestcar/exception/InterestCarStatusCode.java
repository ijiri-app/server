package ijiri.ijiriserver.domain.interestcar.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestCarStatusCode implements StatusCode {

    DUPLICATE_CAR_MODEL(HttpStatus.BAD_REQUEST, "DUPLICATE_CAR_MODEL", "중복된 차종이 포함되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

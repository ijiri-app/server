package ijiri.ijiriserver.domain.interestcar.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestCarStatusCode implements StatusCode {

    UPDATE_SUCCESS(HttpStatus.OK, "INTERESTCAR200", "관심 차종이 저장되었습니다."),
    DUPLICATE_CAR_MODEL(HttpStatus.BAD_REQUEST, "INTERESTCAR4001", "중복된 차종이 포함되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

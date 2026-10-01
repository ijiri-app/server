package ijiri.ijiriserver.domain.part.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PartStatusCode implements StatusCode {

    INVALID_PART(HttpStatus.BAD_REQUEST, "INVALID_PART", "부품은 기존 부품 ID 또는 새 부품의 이름과 분류 중 하나로 입력해 주세요."),
    PART_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "부품을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

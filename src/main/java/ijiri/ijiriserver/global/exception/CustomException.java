package ijiri.ijiriserver.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final StatusCode statusCode;

    public CustomException(StatusCode statusCode) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
    }
}

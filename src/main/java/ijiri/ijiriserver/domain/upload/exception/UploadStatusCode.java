package ijiri.ijiriserver.domain.upload.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadStatusCode implements StatusCode {

    UNSUPPORTED_IMAGE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE", "업로드 URL 을 받을 때 정한 형식의 사진만 올릴 수 있습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "올린 사진을 찾을 수 없거나 이미 사용된 사진입니다."),
    INVALID_UPLOAD_URL(HttpStatus.FORBIDDEN, "FORBIDDEN", "업로드 URL 이 만료되었거나 올바르지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "사진을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

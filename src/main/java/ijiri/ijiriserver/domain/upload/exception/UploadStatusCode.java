package ijiri.ijiriserver.domain.upload.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadStatusCode implements StatusCode {

    UPLOAD_SUCCESS(HttpStatus.CREATED, "UPLOAD201", "사진이 업로드되었습니다."),
    UNSUPPORTED_IMAGE(HttpStatus.BAD_REQUEST, "UPLOAD4001", "JPEG, PNG, WebP, HEIC 사진만 올릴 수 있습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "UPLOAD4002", "올린 사진을 찾을 수 없거나 이미 사용된 사진입니다."),
    EMPTY_IMAGE(HttpStatus.BAD_REQUEST, "UPLOAD4003", "빈 파일은 올릴 수 없습니다."),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD500", "사진 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

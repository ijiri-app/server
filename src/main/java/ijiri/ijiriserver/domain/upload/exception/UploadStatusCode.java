package ijiri.ijiriserver.domain.upload.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadStatusCode implements StatusCode {

    INVALID_IMAGE(
            HttpStatus.BAD_REQUEST,
            "INVALID_IMAGE",
            "사진을 찾을 수 없거나, 이미 사용됐거나, 10MB 를 넘거나, JPEG·WebP 사진이 아닙니다."
    ),
    INVALID_UPLOAD_URL(HttpStatus.FORBIDDEN, "FORBIDDEN", "업로드 URL 이 만료되었거나 올바르지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "사진을 찾을 수 없습니다."),
    STORAGE_ERROR(HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", "사진 저장소와 통신에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

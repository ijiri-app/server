package ijiri.ijiriserver.domain.post.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostStatusCode implements StatusCode {

    CREATE_SUCCESS(HttpStatus.CREATED, "POST_CREATED", "게시물이 등록되었습니다."),
    INVALID_PART_REF(
            HttpStatus.BAD_REQUEST,
            "INVALID_PART_REF",
            "부품 ref·postPartId 가 겹치거나, 유지하는 부품을 다른 부품으로 바꾸거나, 없는 ref·사진을 가리킵니다."
    ),
    MISSING_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "사진의 width, height 가 필요합니다."),
    TAGS_WITHOUT_PARTS(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "태그(images)는 parts 와 함께 보내야 합니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 게시물만 수정·삭제할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "게시물을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

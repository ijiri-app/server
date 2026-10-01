package ijiri.ijiriserver.domain.post.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostStatusCode implements StatusCode {

    CREATE_SUCCESS(HttpStatus.CREATED, "POST201", "게시물이 등록되었습니다."),
    UPDATE_SUCCESS(HttpStatus.OK, "POST2001", "게시물이 수정되었습니다."),
    DELETE_SUCCESS(HttpStatus.OK, "POST2002", "게시물이 삭제되었습니다."),
    DUPLICATE_IMAGE(HttpStatus.BAD_REQUEST, "POST4001", "같은 사진을 두 번 넣을 수 없습니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "POST403", "내 게시물만 수정·삭제할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404", "게시물을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

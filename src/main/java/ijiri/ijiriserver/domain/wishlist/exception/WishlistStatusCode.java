package ijiri.ijiriserver.domain.wishlist.exception;

import ijiri.ijiriserver.global.exception.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WishlistStatusCode implements StatusCode {

    ADD_SUCCESS(HttpStatus.CREATED, "WISHLIST_ADDED", "위시리스트에 담았습니다."),
    ALREADY_ADDED(HttpStatus.OK, "WISHLIST_ALREADY_ADDED", "이미 담긴 부품입니다."),
    WISHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "위시리스트 항목을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

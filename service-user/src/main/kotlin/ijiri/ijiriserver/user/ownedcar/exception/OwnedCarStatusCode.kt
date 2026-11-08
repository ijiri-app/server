package ijiri.ijiriserver.user.ownedcar.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class OwnedCarStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    CREATE_SUCCESS(HttpStatus.CREATED, "OWNED_CAR_CREATED", "보유 차량이 등록되었습니다."),
    OWNED_CAR_LIMIT_EXCEEDED(
        HttpStatus.BAD_REQUEST,
        "OWNED_CAR_LIMIT_EXCEEDED",
        "지금 타는 차는 최대 10대까지 등록할 수 있습니다.",
    ),
    INVALID_MODEL_YEAR(HttpStatus.BAD_REQUEST, "INVALID_MODEL_YEAR", "연식이 세대의 판매 기간을 벗어났습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 차량만 사용할 수 있습니다."),
    OWNED_CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "보유 차량을 찾을 수 없습니다."),
}

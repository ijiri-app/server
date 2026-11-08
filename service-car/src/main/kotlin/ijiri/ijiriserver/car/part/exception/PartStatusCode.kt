package ijiri.ijiriserver.car.part.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class PartStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    INVALID_PART(
        HttpStatus.BAD_REQUEST,
        "INVALID_PART",
        "부품은 기존 부품 ID 또는 새 부품의 이름과 분류 중 하나로 입력해 주세요.",
    ),
    PART_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "부품을 찾을 수 없습니다."),
}

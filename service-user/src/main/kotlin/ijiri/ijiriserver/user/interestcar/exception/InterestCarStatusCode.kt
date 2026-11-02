package ijiri.ijiriserver.user.interestcar.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class InterestCarStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    DUPLICATE_CAR_MODEL(HttpStatus.BAD_REQUEST, "DUPLICATE_CAR_MODEL", "중복된 차종이 포함되어 있습니다."),
    CAR_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "차종을 찾을 수 없습니다."),
}

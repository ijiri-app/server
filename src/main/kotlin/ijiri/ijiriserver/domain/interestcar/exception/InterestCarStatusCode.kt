package ijiri.ijiriserver.domain.interestcar.exception

import ijiri.ijiriserver.global.exception.status_code.StatusCode
import org.springframework.http.HttpStatus

enum class InterestCarStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    DUPLICATE_CAR_MODEL(HttpStatus.BAD_REQUEST, "DUPLICATE_CAR_MODEL", "중복된 차종이 포함되어 있습니다."),
}

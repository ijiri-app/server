package ijiri.ijiriserver.domain.block.exception

import ijiri.ijiriserver.global.exception.status_code.StatusCode
import org.springframework.http.HttpStatus

enum class BlockStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "CANNOT_BLOCK_SELF", "자기 자신은 차단할 수 없습니다."),
}

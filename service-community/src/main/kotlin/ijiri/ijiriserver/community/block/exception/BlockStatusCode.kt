package ijiri.ijiriserver.community.block.exception

import ijiri.ijiriserver.common.exception.StatusCode
import org.springframework.http.HttpStatus

enum class BlockStatusCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : StatusCode {

    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "CANNOT_BLOCK_SELF", "자기 자신은 차단할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "회원을 찾을 수 없습니다."),
}

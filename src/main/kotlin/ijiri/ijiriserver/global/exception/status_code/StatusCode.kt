package ijiri.ijiriserver.global.exception.status_code

import org.springframework.http.HttpStatus

/**
 * 도메인별 상태 코드 enum 은 이 인터페이스를 구현한다. (ex. MemberStatusCode : StatusCode)
 */
interface StatusCode {

    val httpStatus: HttpStatus

    val code: String

    val message: String
}

package ijiri.ijiriserver.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 상태 코드 enum 은 이 인터페이스를 구현한다. (ex. MemberStatusCode implements StatusCode)
 */
public interface StatusCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}

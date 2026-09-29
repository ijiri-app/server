package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;

public interface EmailVerificationService {

    AuthResponse sendCode(String email);

    /**
     * 이메일에 발송된 코드와 일치하고 만료되지 않았는지 확인한 뒤 코드를 삭제한다. 코드는 한 번만 사용할 수 있다.
     */
    void verifyAndConsume(String email, String code);
}

package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;

public interface EmailVerificationService {

    AuthResponse sendCode(String email, VerificationPurpose purpose);

    /**
     * 발송된 코드와 일치하고 만료되지 않았으면 이메일을 인증 완료로 표시한다.
     */
    AuthResponse verifyCode(String email, String code, VerificationPurpose purpose);

    /**
     * 인증 완료된 이메일인지 확인하고 인증 기록을 삭제한다. 한 번의 인증은 한 번의 가입(재설정)에만 쓸 수 있다.
     */
    void consumeVerified(String email, VerificationPurpose purpose);
}

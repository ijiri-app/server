package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;

public interface EmailVerificationService {

    void sendCode(String email, VerificationPurpose purpose);

    /**
     * 발송된 코드와 일치하면 verificationToken(10분, 1회용)을 발급한다.
     * purpose 가 null 이면 이 이메일로 가장 최근에 보낸 코드와 비교한다.
     */
    AuthResponse verifyCode(String email, String code, VerificationPurpose purpose);

    /**
     * verificationToken 을 확인하고 인증 기록을 삭제한다. 한 번의 인증은 한 번의 가입(재설정)에만 쓸 수 있다.
     */
    void consumeVerified(String email, VerificationPurpose purpose, String verificationToken);
}

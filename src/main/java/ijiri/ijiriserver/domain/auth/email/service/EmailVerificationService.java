package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;

public interface EmailVerificationService {

    /**
     * 가입 여부와 무관하게 같은 응답. 이미 가입된 이메일이면 코드 대신 안내 메일을 보낸다.
     */
    void sendSignupCode(String email);

    /**
     * 가입 코드가 맞으면 verificationToken(10분, 1회용)을 발급한다.
     */
    AuthResponse verifySignupCode(String email, String code);

    /**
     * verificationToken 을 확인하고 인증 기록을 삭제한다. 한 번의 인증은 한 번의 가입에만 쓸 수 있다.
     */
    void consumeSignupToken(String email, String verificationToken);

    /**
     * 가입 여부와 무관하게 같은 응답. 활성 이메일 회원에게만 코드를 보낸다.
     */
    void sendResetCode(String email);

    /**
     * 재설정 코드가 맞으면 인증 기록을 삭제한다. 틀린 시도는 횟수에 남는다.
     */
    void consumeResetCode(String email, String code);
}

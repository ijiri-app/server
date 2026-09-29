package ijiri.ijiriserver.domain.auth.email.service;

public interface EmailVerificationService {

    void sendCode(String email);

    void verify(String email, String code);

    /**
     * 인증이 끝난 이메일인지 확인하고 인증 기록을 소모(삭제)한다. 가입 시 한 번만 사용 가능.
     */
    void consumeVerified(String email);
}

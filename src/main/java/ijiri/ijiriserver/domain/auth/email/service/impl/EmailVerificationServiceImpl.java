package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailSender;
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.repository.EmailVerificationRepository;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final long CODE_VALID_MINUTES = 10;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_LENGTH = 6;
    // 영문 대문자 + 숫자로 생성하고, 입력은 대소문자 구분 없이 비교한다
    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationMailSender verificationMailSender;
    private final MemberService memberService;

    // 메일 발송이 실패하면 예외로 코드 저장도 롤백되어, 받지 못한 코드가 남지 않는다
    @Override
    @Transactional
    public void sendCode(String email) {
        if (memberService.existsEmailMember(email)) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(CODE_VALID_MINUTES);
        String code = generateCode();

        emailVerificationRepository.findByEmail(email)
                .ifPresentOrElse(
                        verification -> {
                            if (!verification.canResend(now, RESEND_COOLDOWN_SECONDS)) {
                                throw new CustomException(AuthStatusCode.VERIFICATION_RESEND_TOO_SOON);
                            }
                            verification.resend(code, now, expiresAt);
                        },
                        () -> emailVerificationRepository.save(EmailVerification.of(email, code, now, expiresAt))
                );

        verificationMailSender.send(email, code, CODE_VALID_MINUTES);
    }

    // 틀린 시도도 횟수가 남아야 하므로 noRollbackFor 로 예외가 나도 attemptCount 증가분은 커밋한다
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public void verifyAndConsume(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        // 이 이메일로 발송된 코드가 없으면 다른 이메일의 코드를 넣은 경우도 포함해 일치하지 않는 것으로 본다
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_VERIFICATION_CODE));

        if (verification.isExpired(now)) {
            throw new CustomException(AuthStatusCode.VERIFICATION_CODE_EXPIRED);
        }
        if (verification.hasExceededAttempts(MAX_ATTEMPTS)) {
            throw new CustomException(AuthStatusCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        if (!verification.matches(code)) {
            throw new CustomException(AuthStatusCode.INVALID_VERIFICATION_CODE);
        }
        emailVerificationRepository.delete(verification);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARACTERS.charAt(RANDOM.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }
}

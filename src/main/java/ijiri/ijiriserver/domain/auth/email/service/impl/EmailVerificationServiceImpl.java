package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailClient;
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.repository.EmailVerificationRepository;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final long CODE_VALID_MINUTES = 5;
    // 코드 확인 후 비밀번호/닉네임 입력까지 허용하는 시간
    private static final long SIGNUP_WINDOW_MINUTES = 30;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_LENGTH = 6;
    private static final String CODE_CHARACTERS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REGISTERED_NOTICE_KEY_PREFIX = "verification:registered:";

    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationMailClient verificationMailClient;
    private final MemberService memberService;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    // 가입된 이메일도 응답(성공/재발송 대기)은 미가입 이메일과 똑같이 두어 가입 여부가 드러나지 않게 한다.
    // 메일 발송은 트랜잭션 밖에서 해 SMTP 응답을 기다리는 동안 DB 커넥션을 잡지 않는다
    @Override
    public AuthResponse sendCode(String email) {
        Optional<Member> registered = memberService.findEmailMember(email);
        if (registered.isPresent()) {
            sendAlreadyRegisteredNotice(email, registered.get().isWithdrawn());
            return codeSentResponse();
        }

        String code = generateCode();
        EmailVerification saved = transactionTemplate.execute(status -> saveCode(email, code));

        // 발송에 실패하면 받지 못한 코드가 남아 재발송 대기에 걸리지 않도록 지운다
        try {
            verificationMailClient.sendCode(email, code, CODE_VALID_MINUTES);
        } catch (CustomException e) {
            emailVerificationRepository.delete(saved);
            throw e;
        }
        return codeSentResponse();
    }

    // 틀린 시도도 횟수가 남아야 하므로 noRollbackFor 로 예외가 나도 attemptCount 증가분은 커밋한다
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public AuthResponse verifyCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
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
        verification.markVerified(now, now.plusMinutes(SIGNUP_WINDOW_MINUTES));
        return AuthResponse.message(AuthStatusCode.EMAIL_VERIFIED.getMessage());
    }

    @Override
    @Transactional
    public void consumeVerified(String email) {
        if (emailVerificationRepository.deleteVerified(email, LocalDateTime.now(clock)) == 0) {
            throw new CustomException(AuthStatusCode.EMAIL_NOT_VERIFIED);
        }
    }

    // 미가입 이메일의 재발송 대기와 같은 주기·같은 에러를 적용한다
    private void sendAlreadyRegisteredNotice(String email, boolean withdrawn) {
        boolean acquired = rateLimiter.tryAcquire(
                REGISTERED_NOTICE_KEY_PREFIX + email,
                1,
                Duration.ofSeconds(RESEND_COOLDOWN_SECONDS)
        );
        if (!acquired) {
            throw new CustomException(AuthStatusCode.VERIFICATION_RESEND_TOO_SOON);
        }
        if (withdrawn) {
            verificationMailClient.sendWithdrawnAccount(email);
        } else {
            verificationMailClient.sendAlreadyRegistered(email);
        }
    }

    // 기존 행은 잠근 뒤 재발송 대기를 확인하고 갱신한다.
    // 같은 이메일로 처음 발송 요청이 동시에 들어오면 잠글 행이 없어 한쪽은 유니크 제약에 걸린다
    private EmailVerification saveCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusMinutes(CODE_VALID_MINUTES);
        Optional<EmailVerification> existing = emailVerificationRepository.findByEmailForUpdate(email);
        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            if (!verification.canResend(now, RESEND_COOLDOWN_SECONDS)) {
                throw new CustomException(AuthStatusCode.VERIFICATION_RESEND_TOO_SOON);
            }
            verification.resend(code, now, expiresAt);
            return verification;
        }
        try {
            return emailVerificationRepository.saveAndFlush(EmailVerification.of(email, code, now, expiresAt));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(AuthStatusCode.VERIFICATION_RESEND_TOO_SOON);
        }
    }

    private AuthResponse codeSentResponse() {
        return AuthResponse.verificationCodeSent(Duration.ofMinutes(CODE_VALID_MINUTES).toSeconds());
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARACTERS.charAt(RANDOM.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }
}

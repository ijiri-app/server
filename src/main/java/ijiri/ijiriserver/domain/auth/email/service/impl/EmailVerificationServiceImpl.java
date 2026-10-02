package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailClient;
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import ijiri.ijiriserver.domain.auth.email.repository.EmailVerificationRepository;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
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
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final long CODE_VALID_MINUTES = 5;
    // 가입 코드 확인 후 가입까지 허용하는 시간 = verificationToken 유효 시간
    private static final long TOKEN_VALID_MINUTES = 10;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_LENGTH = 6;
    private static final String CODE_CHARACTERS = "0123456789";
    private static final String TOKEN_PREFIX = "vt_";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String NOTICE_KEY_PREFIX = "verification:notice:";
    // 코드당 5번 + 1분마다 재발송이면 IP 를 바꿔 가며 6자리 코드를 계속 맞혀 볼 수 있으므로 이메일별 하루 시도 수를 묶는다
    private static final String ATTEMPT_KEY_PREFIX = "verification:attempt:";
    private static final int MAX_ATTEMPTS_PER_DAY = 10;
    private static final Duration ATTEMPT_LIMIT_PERIOD = Duration.ofDays(1);

    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationMailClient verificationMailClient;
    private final MemberService memberService;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    // 가입된 이메일도 응답(성공/재발송 대기)은 미가입 이메일과 똑같이 두고 안내 메일만 보내 가입 여부가 드러나지 않게 한다
    @Override
    public void sendSignupCode(String email) {
        Optional<Member> registered = memberService.findEmailMember(email);
        if (registered.isPresent()) {
            acquireNoticeCooldown(email, VerificationPurpose.SIGNUP);
            if (registered.get().isWithdrawn()) {
                verificationMailClient.sendWithdrawnAccount(email);
            } else {
                verificationMailClient.sendAlreadyRegistered(email);
            }
            return;
        }
        sendCode(email, VerificationPurpose.SIGNUP);
    }

    // 틀린 시도도 횟수가 남아야 하므로 noRollbackFor 로 예외가 나도 attemptCount 증가분은 커밋한다
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public AuthResponse verifySignupCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerification verification = checkCode(email, VerificationPurpose.SIGNUP, code, now);
        String token = TOKEN_PREFIX + randomToken();
        verification.markVerified(now, RefreshToken.hash(token), now.plusMinutes(TOKEN_VALID_MINUTES));
        return AuthResponse.verified(token);
    }

    @Override
    @Transactional
    public void consumeSignupToken(String email, String verificationToken) {
        int deleted = emailVerificationRepository.deleteVerified(
                email,
                VerificationPurpose.SIGNUP,
                RefreshToken.hash(verificationToken),
                LocalDateTime.now(clock)
        );
        if (deleted == 0) {
            throw new CustomException(AuthStatusCode.INVALID_VERIFICATION_TOKEN);
        }
    }

    // 가입 여부가 드러나지 않도록 활성 이메일 회원이 아니어도 같은 응답(재발송 대기 포함)을 주고 메일만 보내지 않는다
    @Override
    public void sendResetCode(String email) {
        if (memberService.findEmailMember(email).filter(member -> !member.isWithdrawn()).isEmpty()) {
            acquireNoticeCooldown(email, VerificationPurpose.RESET_PASSWORD);
            return;
        }
        sendCode(email, VerificationPurpose.RESET_PASSWORD);
    }

    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public void consumeResetCode(String email, String code) {
        EmailVerification verification = checkCode(
                email,
                VerificationPurpose.RESET_PASSWORD,
                code,
                LocalDateTime.now(clock)
        );
        emailVerificationRepository.delete(verification);
    }

    // 메일 발송은 트랜잭션 밖에서 해 SMTP 응답을 기다리는 동안 DB 커넥션을 잡지 않는다
    private void sendCode(String email, VerificationPurpose purpose) {
        String code = generateCode();
        EmailVerification saved = transactionTemplate.execute(status -> saveCode(email, purpose, code));
        // 발송에 실패하면 받지 못한 코드가 남아 재발송 대기에 걸리지 않도록 지운다
        try {
            verificationMailClient.sendCode(email, code, CODE_VALID_MINUTES);
        } catch (CustomException e) {
            emailVerificationRepository.delete(saved);
            throw e;
        }
    }

    // 이 이메일로 발송된 코드가 없으면 다른 이메일의 코드를 넣은 경우도 포함해 일치하지 않는 것으로 본다.
    // 이미 확인했거나, 만료됐거나, 5번 틀린 코드는 무효
    private EmailVerification checkCode(String email, VerificationPurpose purpose, String code, LocalDateTime now) {
        rateLimiter.check(ATTEMPT_KEY_PREFIX + purpose + ":" + email, MAX_ATTEMPTS_PER_DAY, ATTEMPT_LIMIT_PERIOD);
        EmailVerification verification = emailVerificationRepository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> invalidCode(0));
        boolean usable = !verification.isVerified()
                && !verification.isExpired(now)
                && !verification.hasExceededAttempts(MAX_ATTEMPTS);
        if (!usable) {
            throw invalidCode(0);
        }
        if (!verification.matches(code)) {
            throw invalidCode(verification.remainingAttempts(MAX_ATTEMPTS));
        }
        return verification;
    }

    // 코드를 발송하지 않는 경우에도 실제 발송과 같은 재발송 대기·같은 에러를 적용한다
    private void acquireNoticeCooldown(String email, VerificationPurpose purpose) {
        String key = NOTICE_KEY_PREFIX + purpose + ":" + email;
        if (!rateLimiter.tryAcquire(key, 1, Duration.ofSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw resendTooSoon(rateLimiter.retryAfterSeconds(key));
        }
    }

    // 기존 행은 잠근 뒤 재발송 대기를 확인하고 갱신한다.
    // 같은 이메일로 처음 발송 요청이 동시에 들어오면 잠글 행이 없어 한쪽은 유니크 제약에 걸린다
    private EmailVerification saveCode(String email, VerificationPurpose purpose, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusMinutes(CODE_VALID_MINUTES);
        Optional<EmailVerification> existing = emailVerificationRepository.findByEmailAndPurposeForUpdate(
                email,
                purpose
        );
        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            if (!verification.canResend(now, RESEND_COOLDOWN_SECONDS)) {
                throw resendTooSoon(verification.secondsUntilResend(now, RESEND_COOLDOWN_SECONDS));
            }
            verification.resend(code, now, expiresAt);
            return verification;
        }
        try {
            return emailVerificationRepository.saveAndFlush(
                    EmailVerification.of(email, purpose, code, now, expiresAt)
            );
        } catch (DataIntegrityViolationException e) {
            throw resendTooSoon(RESEND_COOLDOWN_SECONDS);
        }
    }

    private CustomException invalidCode(int remainingAttempts) {
        return new CustomException(
                AuthStatusCode.INVALID_VERIFICATION_CODE,
                Map.of("remainingAttempts", remainingAttempts)
        );
    }

    private CustomException resendTooSoon(long retryAfterSeconds) {
        return new CustomException(
                AuthStatusCode.VERIFICATION_RESEND_TOO_SOON,
                Map.of("retryAfterSeconds", retryAfterSeconds)
        );
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARACTERS.charAt(RANDOM.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

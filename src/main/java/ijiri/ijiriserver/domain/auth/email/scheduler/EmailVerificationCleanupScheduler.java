package ijiri.ijiriserver.domain.auth.email.scheduler;

import ijiri.ijiriserver.domain.auth.email.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 인증만 하고 가입하지 않았거나, 코드만 받고 방치된 만료 인증 기록 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final EmailVerificationRepository emailVerificationRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    public void deleteExpired() {
        int deleted = emailVerificationRepository.deleteAllExpired(LocalDateTime.now(clock));
        log.info("Deleted {} expired email verifications", deleted);
    }
}

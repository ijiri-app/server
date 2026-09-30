package ijiri.ijiriserver.domain.auth.token.scheduler;

import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 로그아웃/재발급 없이 방치된(앱 삭제, 장기 미접속) 만료 refresh token 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now(clock));
        log.info("Deleted {} expired refresh tokens", deleted);
    }
}

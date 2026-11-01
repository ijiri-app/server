package ijiri.ijiriserver.domain.auth.token.adapter.out.persistence;

import ijiri.ijiriserver.domain.auth.token.application.port.out.RefreshTokenPort;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public int deleteValidByTokenHash(String tokenHash, LocalDateTime now) {
        return refreshTokenJpaRepository.deleteValidByTokenHash(tokenHash, now);
    }

    @Override
    public boolean existsBySessionIdAndExpiresAtAfter(String sessionId, LocalDateTime now) {
        return refreshTokenJpaRepository.existsBySessionIdAndExpiresAtAfter(sessionId, now);
    }

    @Override
    public int deleteByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.deleteByTokenHash(tokenHash);
    }

    @Override
    public int deleteAllByMemberId(Long memberId) {
        return refreshTokenJpaRepository.deleteAllByMemberId(memberId);
    }

    @Override
    public int deleteAllExpired(LocalDateTime now) {
        return refreshTokenJpaRepository.deleteAllExpired(now);
    }

    @Override
    public RefreshToken save(RefreshToken entity) {
        return refreshTokenJpaRepository.save(entity);
    }
}

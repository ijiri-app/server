package ijiri.ijiriserver.domain.auth.token.application.port.out;

import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;

import java.time.LocalDateTime;

public interface RefreshTokenPort {

    int deleteValidByTokenHash(String tokenHash, LocalDateTime now);

    boolean existsBySessionIdAndExpiresAtAfter(String sessionId, LocalDateTime now);

    int deleteByTokenHash(String tokenHash);

    int deleteAllByMemberId(Long memberId);

    int deleteAllExpired(LocalDateTime now);

    RefreshToken save(RefreshToken entity);
}

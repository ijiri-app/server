package ijiri.ijiriserver.user.auth.token.adapter.out.persistence

import ijiri.ijiriserver.user.auth.token.application.port.out.RefreshTokenPort
import ijiri.ijiriserver.user.auth.token.entity.RefreshToken
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RefreshTokenPersistenceAdapter(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenPort {

    override fun deleteValidByTokenHash(tokenHash: String, now: LocalDateTime): Int =
        refreshTokenJpaRepository.deleteValidByTokenHash(tokenHash, now)

    override fun existsBySessionIdAndExpiresAtAfter(sessionId: String, now: LocalDateTime): Boolean =
        refreshTokenJpaRepository.existsBySessionIdAndExpiresAtAfter(sessionId, now)

    override fun deleteByTokenHash(tokenHash: String): Int = refreshTokenJpaRepository.deleteByTokenHash(tokenHash)

    override fun deleteAllByMemberId(memberId: Long): Int = refreshTokenJpaRepository.deleteAllByMemberId(memberId)

    override fun deleteAllExpired(now: LocalDateTime): Int = refreshTokenJpaRepository.deleteAllExpired(now)

    override fun save(entity: RefreshToken): RefreshToken = refreshTokenJpaRepository.save(entity)
}

package ijiri.ijiriserver.user.auth.token.application.port.out

import ijiri.ijiriserver.user.auth.token.entity.RefreshToken
import java.time.LocalDateTime

interface RefreshTokenPort {

    fun deleteValidByTokenHash(tokenHash: String, now: LocalDateTime): Int

    fun existsBySessionIdAndExpiresAtAfter(sessionId: String, now: LocalDateTime): Boolean

    fun deleteByTokenHash(tokenHash: String): Int

    fun deleteAllByMemberId(memberId: Long): Int

    fun deleteAllExpired(now: LocalDateTime): Int

    fun save(entity: RefreshToken): RefreshToken
}

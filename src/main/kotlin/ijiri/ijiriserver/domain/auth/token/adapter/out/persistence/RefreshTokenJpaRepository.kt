package ijiri.ijiriserver.domain.auth.token.adapter.out.persistence

import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {

    // 조회 후 삭제 대신 한 번에 지우고 삭제 건수로 판단해, 같은 토큰의 동시 재발급 중 하나만 성공하게 한다
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash AND r.expiresAt > :now")
    fun deleteValidByTokenHash(@Param("tokenHash") tokenHash: String, @Param("now") now: LocalDateTime): Int

    fun existsBySessionIdAndExpiresAtAfter(sessionId: String, now: LocalDateTime): Boolean

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    fun deleteByTokenHash(@Param("tokenHash") tokenHash: String): Int

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.memberId = :memberId")
    fun deleteAllByMemberId(@Param("memberId") memberId: Long): Int

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    fun deleteAllExpired(@Param("now") now: LocalDateTime): Int
}

package ijiri.ijiriserver.domain.auth.token.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.HexFormat

/**
 * 회원당 한 줄(동시 접속 차단, member_id 유니크). 로그인/재발급 때마다 기존 토큰을 지우고 새로 저장한다.
 * DB 유출 시 바로 쓸 수 없도록 원문 대신 SHA-256 해시만 저장한다.
 */
@Entity
@Table(
    name = "refresh_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_refresh_token_member_id", columnNames = ["member_id"]),
        UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = ["token_hash"]),
        UniqueConstraint(name = "uk_refresh_token_session_id", columnNames = ["session_id"]),
    ],
)
class RefreshToken(
    memberId: Long,
    tokenHash: String,
    expiresAt: LocalDateTime,
    sessionId: String,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    var tokenHash: String = tokenHash
        protected set

    @Column(name = "expires_at", nullable = false, updatable = false)
    var expiresAt: LocalDateTime = expiresAt
        protected set

    // 이 로그인 세션의 id. 같은 세션의 access token 이 sid 클레임으로 들고 있다
    @Column(name = "session_id", nullable = false, length = 36, updatable = false)
    var sessionId: String = sessionId
        protected set

    companion object {
        // 원문 토큰이 엔티티에 남지 않도록 생성 시점에 해시로 바꾼다
        fun of(memberId: Long, sessionId: String, token: String, expiresAt: LocalDateTime): RefreshToken =
            RefreshToken(
                memberId = memberId,
                sessionId = sessionId,
                tokenHash = hash(token),
                expiresAt = expiresAt,
            )

        fun hash(token: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
            return HexFormat.of().formatHex(digest)
        }
    }
}

package ijiri.ijiriserver.domain.auth.token.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 회원당 한 줄(동시 접속 차단). 로그인/재발급 때마다 기존 토큰을 지우고 새로 저장한다.
 * DB 유출 시 바로 쓸 수 없도록 원문 대신 SHA-256 해시만 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_session_id", columnNames = "session_id"),
        indexes = @Index(name = "idx_refresh_token_member_id", columnList = "member_id")
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    // 이 로그인 세션의 id. 같은 세션의 access token 이 sid 클레임으로 들고 있다.
    // 컬럼 추가 전에 저장된 행은 null 이며, 그 세션은 유효하지 않은 것으로 본다
    @Column(name = "session_id", length = 36, updatable = false)
    private String sessionId;

    // 원문 토큰이 엔티티에 남지 않도록 생성 시점에 해시로 바꾼다
    public static RefreshToken of(Long memberId, String sessionId, String token, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .memberId(memberId)
                .sessionId(sessionId)
                .tokenHash(hash(token))
                .expiresAt(expiresAt)
                .build();
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

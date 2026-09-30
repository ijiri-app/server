package ijiri.ijiriserver.domain.auth.token.repository;

import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 조회 후 삭제 대신 한 번에 지우고 삭제 건수로 판단해, 같은 토큰의 동시 재발급 중 하나만 성공하게 한다
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash AND r.expiresAt > :now")
    int deleteValidByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Query("SELECT r.id FROM RefreshToken r WHERE r.memberId = :memberId ORDER BY r.id DESC")
    List<Long> findIdsByMemberIdNewestFirst(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.id IN :ids")
    int deleteAllByIdIn(@Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}

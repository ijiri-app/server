package ijiri.ijiriserver.domain.auth.email.repository;

import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);

    // 조회 후 삭제 대신 한 번에 지우고 삭제 건수로 판단해, 한 번의 인증으로 동시에 두 번 가입하지 못하게 한다
    @Modifying
    @Query("""
            DELETE FROM EmailVerification v
            WHERE v.email = :email AND v.verifiedAt IS NOT NULL AND v.expiresAt > :now
            """)
    int deleteVerified(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}

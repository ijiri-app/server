package ijiri.ijiriserver.domain.auth.email.repository;

import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndPurpose(String email, VerificationPurpose purpose);

    Optional<EmailVerification> findFirstByEmailOrderBySentAtDesc(String email);

    // 같은 이메일로 재발송 요청이 동시에 들어와도 재발송 대기 확인과 갱신이 한 번에 하나씩만 일어나게 한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email AND v.purpose = :purpose")
    Optional<EmailVerification> findByEmailAndPurposeForUpdate(
            @Param("email") String email,
            @Param("purpose") VerificationPurpose purpose
    );

    // 조회 후 삭제 대신 한 번에 지우고 삭제 건수로 판단해, 한 번의 인증을 동시에 두 번 쓰지 못하게 한다
    @Modifying
    @Query("""
            DELETE FROM EmailVerification v
            WHERE v.email = :email AND v.purpose = :purpose AND v.verificationTokenHash = :tokenHash
              AND v.expiresAt > :now
            """)
    int deleteVerified(
            @Param("email") String email,
            @Param("purpose") VerificationPurpose purpose,
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}

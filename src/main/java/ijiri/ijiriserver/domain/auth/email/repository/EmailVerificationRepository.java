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

    void deleteByEmail(String email);

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}

package ijiri.ijiriserver.domain.auth.email.application.port.out;

import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationPort {

    Optional<EmailVerification> findByEmailAndPurpose(String email, VerificationPurpose purpose);

    Optional<EmailVerification> findByEmailAndPurposeForUpdate(String email, VerificationPurpose purpose);

    int deleteVerified(String email, VerificationPurpose purpose, String tokenHash, LocalDateTime now);

    int deleteAllExpired(LocalDateTime now);

    void delete(EmailVerification entity);

    EmailVerification saveAndFlush(EmailVerification entity);
}

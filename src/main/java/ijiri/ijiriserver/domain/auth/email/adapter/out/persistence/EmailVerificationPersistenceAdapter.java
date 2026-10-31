package ijiri.ijiriserver.domain.auth.email.adapter.out.persistence;

import ijiri.ijiriserver.domain.auth.email.application.port.out.EmailVerificationPort;
import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailVerificationPersistenceAdapter implements EmailVerificationPort {

    private final EmailVerificationJpaRepository emailVerificationJpaRepository;

    @Override
    public Optional<EmailVerification> findByEmailAndPurpose(String email, VerificationPurpose purpose) {
        return emailVerificationJpaRepository.findByEmailAndPurpose(email, purpose);
    }

    @Override
    public Optional<EmailVerification> findByEmailAndPurposeForUpdate(String email, VerificationPurpose purpose) {
        return emailVerificationJpaRepository.findByEmailAndPurposeForUpdate(email, purpose);
    }

    @Override
    public int deleteVerified(String email, VerificationPurpose purpose, String tokenHash, LocalDateTime now) {
        return emailVerificationJpaRepository.deleteVerified(email, purpose, tokenHash, now);
    }

    @Override
    public int deleteAllExpired(LocalDateTime now) {
        return emailVerificationJpaRepository.deleteAllExpired(now);
    }

    @Override
    public void delete(EmailVerification entity) {
        emailVerificationJpaRepository.delete(entity);
    }

    @Override
    public EmailVerification saveAndFlush(EmailVerification entity) {
        return emailVerificationJpaRepository.saveAndFlush(entity);
    }
}

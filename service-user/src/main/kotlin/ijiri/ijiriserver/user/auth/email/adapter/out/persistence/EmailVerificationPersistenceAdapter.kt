package ijiri.ijiriserver.user.auth.email.adapter.out.persistence

import ijiri.ijiriserver.user.auth.email.application.port.out.EmailVerificationPort
import ijiri.ijiriserver.user.auth.email.entity.EmailVerification
import ijiri.ijiriserver.user.auth.email.entity.VerificationPurpose
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EmailVerificationPersistenceAdapter(
    private val emailVerificationJpaRepository: EmailVerificationJpaRepository,
) : EmailVerificationPort {

    override fun findByEmailAndPurpose(email: String, purpose: VerificationPurpose): EmailVerification? =
        emailVerificationJpaRepository.findByEmailAndPurpose(email, purpose)

    override fun findByEmailAndPurposeForUpdate(email: String, purpose: VerificationPurpose): EmailVerification? =
        emailVerificationJpaRepository.findByEmailAndPurposeForUpdate(email, purpose)

    override fun deleteVerified(
        email: String,
        purpose: VerificationPurpose,
        tokenHash: String,
        now: LocalDateTime,
    ): Int = emailVerificationJpaRepository.deleteVerified(email, purpose, tokenHash, now)

    override fun deleteAllExpired(now: LocalDateTime): Int = emailVerificationJpaRepository.deleteAllExpired(now)

    override fun delete(entity: EmailVerification) {
        emailVerificationJpaRepository.delete(entity)
    }

    override fun saveAndFlush(entity: EmailVerification): EmailVerification =
        emailVerificationJpaRepository.saveAndFlush(entity)
}

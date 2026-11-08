package ijiri.ijiriserver.user.auth.email.application.port.out

import ijiri.ijiriserver.user.auth.email.entity.EmailVerification
import ijiri.ijiriserver.user.auth.email.entity.VerificationPurpose
import java.time.LocalDateTime

interface EmailVerificationPort {

    fun findByEmailAndPurpose(email: String, purpose: VerificationPurpose): EmailVerification?

    fun findByEmailAndPurposeForUpdate(email: String, purpose: VerificationPurpose): EmailVerification?

    fun deleteVerified(email: String, purpose: VerificationPurpose, tokenHash: String, now: LocalDateTime): Int

    fun deleteAllExpired(now: LocalDateTime): Int

    fun delete(entity: EmailVerification)

    fun saveAndFlush(entity: EmailVerification): EmailVerification
}

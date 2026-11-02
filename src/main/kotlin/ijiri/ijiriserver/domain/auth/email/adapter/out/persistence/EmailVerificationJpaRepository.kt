package ijiri.ijiriserver.domain.auth.email.adapter.out.persistence

import ijiri.ijiriserver.domain.auth.email.entity.EmailVerification
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EmailVerificationJpaRepository : JpaRepository<EmailVerification, Long> {

    fun findByEmailAndPurpose(email: String, purpose: VerificationPurpose): EmailVerification?

    // 같은 이메일로 재발송 요청이 동시에 들어와도 재발송 대기 확인과 갱신이 한 번에 하나씩만 일어나게 한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email AND v.purpose = :purpose")
    fun findByEmailAndPurposeForUpdate(
        @Param("email") email: String,
        @Param("purpose") purpose: VerificationPurpose,
    ): EmailVerification?

    // 조회 후 삭제 대신 한 번에 지우고 삭제 건수로 판단해, 한 번의 인증을 동시에 두 번 쓰지 못하게 한다
    @Modifying
    @Query(
        """
        DELETE FROM EmailVerification v
        WHERE v.email = :email AND v.purpose = :purpose AND v.verificationTokenHash = :tokenHash
          AND v.expiresAt > :now
        """,
    )
    fun deleteVerified(
        @Param("email") email: String,
        @Param("purpose") purpose: VerificationPurpose,
        @Param("tokenHash") tokenHash: String,
        @Param("now") now: LocalDateTime,
    ): Int

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.expiresAt < :now")
    fun deleteAllExpired(@Param("now") now: LocalDateTime): Int
}

package ijiri.ijiriserver.domain.member.entity

import ijiri.ijiriserver.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_provider_provider_member_id",
            columnNames = ["provider", "provider_member_id"],
        ),
        UniqueConstraint(name = "uk_member_nickname", columnNames = ["nickname"]),
    ],
    indexes = [Index(name = "idx_member_deleted_at", columnList = "deleted_at")],
)
class Member(
    nickname: String,
    provider: Provider,
    providerMemberId: String,
    role: Role,
    email: String? = null,
    password: String? = null,
    profileImageUrl: String? = null,
    statusMessage: String? = null,
    deletedAt: LocalDateTime? = null,
    suspendedUntil: LocalDateTime? = null,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    // 소셜 회원은 이메일 제공에 동의하지 않았거나 미인증이면 null
    @Column(name = "email")
    var email: String? = email
        protected set

    @Column(name = "nickname", nullable = false, length = 100)
    var nickname: String = nickname
        protected set

    // 이메일 회원만 사용하는 BCrypt 해시. 소셜 회원은 null
    @Column(name = "password", length = 60)
    var password: String? = password
        protected set

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = profileImageUrl
        protected set

    @Column(name = "status_message", length = 50)
    var statusMessage: String? = statusMessage
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20, updatable = false)
    var provider: Provider = provider
        protected set

    @Column(name = "provider_member_id", nullable = false, updatable = false)
    var providerMemberId: String = providerMemberId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: Role = role
        protected set

    // 탈퇴 시각. null 이면 활성 회원. 탈퇴 후 보관 기간이 지나면 MemberPurgeScheduler 가 행을 완전히 지운다
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = deletedAt
        protected set

    // 관리자가 정지한 기한. 이 시각 전에는 로그인(토큰 발급)할 수 없다
    @Column(name = "suspended_until")
    var suspendedUntil: LocalDateTime? = suspendedUntil
        protected set

    // 재가입 제한에는 provider + providerMemberId 만 쓰므로, 보관 기간 동안 연락처인 이메일은 남기지 않는다
    fun withdraw(now: LocalDateTime) {
        this.deletedAt = now
        this.email = null
    }

    fun updateProfile(nickname: String, profileImageUrl: String?, statusMessage: String?) {
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
        this.statusMessage = statusMessage
    }

    fun isAdmin(): Boolean {
        return role == Role.ADMIN
    }

    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    fun suspend(until: LocalDateTime) {
        this.suspendedUntil = until
    }

    fun isSuspended(now: LocalDateTime): Boolean {
        val until = suspendedUntil
        return until != null && until.isAfter(now)
    }

    fun isWithdrawn(): Boolean {
        return deletedAt != null
    }
}

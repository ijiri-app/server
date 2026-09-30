package ijiri.ijiriserver.domain.member.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_provider_provider_member_id",
                columnNames = {"provider", "provider_member_id"}
        ),
        indexes = @Index(name = "idx_member_deleted_at", columnList = "deleted_at")
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소셜 회원은 이메일 제공에 동의하지 않았거나 미인증이면 null
    @Column(name = "email")
    private String email;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    // 이메일 회원만 사용하는 BCrypt 해시. 소셜 회원은 null
    @Column(name = "password", length = 60)
    private String password;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20, updatable = false)
    private Provider provider;

    @Column(name = "provider_member_id", nullable = false, updatable = false)
    private String providerMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    // 탈퇴 시각. null 이면 활성 회원. 탈퇴 후 보관 기간이 지나면 MemberPurgeScheduler 가 행을 완전히 지운다
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void withdraw(LocalDateTime now) {
        this.deletedAt = now;
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }
}

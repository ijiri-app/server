package ijiri.ijiriserver.domain.member.adapter.out.persistence

import ijiri.ijiriserver.domain.member.entity.Member
import ijiri.ijiriserver.domain.member.entity.Provider
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MemberJpaRepository : JpaRepository<Member, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Member?

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<Member>

    // 탈퇴 회원도 포함해 조회한다. 보관 기간 동안은 같은 계정으로 재가입할 수 없어야 하기 때문
    fun findByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Member?

    fun existsByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    @Query("SELECT m.id FROM Member m WHERE m.deletedAt < :cutoff AND m.id > :afterId ORDER BY m.id")
    fun findIdsWithdrawnBefore(
        @Param("cutoff") cutoff: LocalDateTime,
        @Param("afterId") afterId: Long,
        limit: Limit,
    ): List<Long>
}

package ijiri.ijiriserver.user.member.application.port.out

import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import org.springframework.data.domain.Limit
import java.time.LocalDateTime

interface MemberPort {

    fun findByIdAndDeletedAtIsNull(id: Long): Member?

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<Member>

    fun findByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Member?

    fun existsByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun findIdsWithdrawnBefore(cutoff: LocalDateTime, afterId: Long, limit: Limit): List<Long>

    fun delete(entity: Member)

    fun findById(id: Long): Member?

    fun save(entity: Member): Member

    fun saveAndFlush(entity: Member): Member
}

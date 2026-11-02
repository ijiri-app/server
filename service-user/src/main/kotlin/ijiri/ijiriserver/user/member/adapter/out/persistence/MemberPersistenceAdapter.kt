package ijiri.ijiriserver.user.member.adapter.out.persistence

import ijiri.ijiriserver.user.member.application.port.out.MemberPort
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import org.springframework.data.domain.Limit
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MemberPersistenceAdapter(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberPort {

    override fun findByIdAndDeletedAtIsNull(id: Long): Member? {
        return memberJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<Member> {
        return memberJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun findByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Member? {
        return memberJpaRepository.findByProviderAndProviderMemberId(provider, providerMemberId)
    }

    override fun existsByProviderAndProviderMemberId(provider: Provider, providerMemberId: String): Boolean {
        return memberJpaRepository.existsByProviderAndProviderMemberId(provider, providerMemberId)
    }

    override fun existsByNickname(nickname: String): Boolean {
        return memberJpaRepository.existsByNickname(nickname)
    }

    override fun findIdsWithdrawnBefore(cutoff: LocalDateTime, afterId: Long, limit: Limit): List<Long> {
        return memberJpaRepository.findIdsWithdrawnBefore(cutoff, afterId, limit)
    }

    override fun delete(entity: Member) {
        memberJpaRepository.delete(entity)
    }

    override fun findById(id: Long): Member? {
        return memberJpaRepository.findByIdOrNull(id)
    }

    override fun save(entity: Member): Member {
        return memberJpaRepository.save(entity)
    }

    override fun saveAndFlush(entity: Member): Member {
        return memberJpaRepository.saveAndFlush(entity)
    }
}

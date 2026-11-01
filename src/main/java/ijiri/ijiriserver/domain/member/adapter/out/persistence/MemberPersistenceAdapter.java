package ijiri.ijiriserver.domain.member.adapter.out.persistence;

import ijiri.ijiriserver.domain.member.application.port.out.MemberPort;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements MemberPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findByIdAndDeletedAtIsNull(Long id) {
        return memberJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Member> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids) {
        return memberJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Optional<Member> findByProviderAndProviderMemberId(Provider provider, String providerMemberId) {
        return memberJpaRepository.findByProviderAndProviderMemberId(provider, providerMemberId);
    }

    @Override
    public boolean existsByProviderAndProviderMemberId(Provider provider, String providerMemberId) {
        return memberJpaRepository.existsByProviderAndProviderMemberId(provider, providerMemberId);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    @Override
    public List<Long> findIdsWithdrawnBefore(LocalDateTime cutoff, Long afterId, Limit limit) {
        return memberJpaRepository.findIdsWithdrawnBefore(cutoff, afterId, limit);
    }

    @Override
    public void delete(Member entity) {
        memberJpaRepository.delete(entity);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id);
    }

    @Override
    public Member save(Member entity) {
        return memberJpaRepository.save(entity);
    }

    @Override
    public Member saveAndFlush(Member entity) {
        return memberJpaRepository.saveAndFlush(entity);
    }
}

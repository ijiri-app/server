package ijiri.ijiriserver.domain.member.application.port.out;

import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberPort {

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    List<Member> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    Optional<Member> findByProviderAndProviderMemberId(Provider provider, String providerMemberId);

    boolean existsByProviderAndProviderMemberId(Provider provider, String providerMemberId);

    boolean existsByNickname(String nickname);

    List<Long> findIdsWithdrawnBefore(LocalDateTime cutoff, Long afterId, Limit limit);

    void delete(Member entity);

    Optional<Member> findById(Long id);

    Member save(Member entity);

    Member saveAndFlush(Member entity);
}

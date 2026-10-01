package ijiri.ijiriserver.domain.member.repository;

import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    List<Member> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    // 탈퇴 회원도 포함해 조회한다. 보관 기간 동안은 같은 계정으로 재가입할 수 없어야 하기 때문
    Optional<Member> findByProviderAndProviderMemberId(Provider provider, String providerMemberId);

    boolean existsByProviderAndProviderMemberId(Provider provider, String providerMemberId);

    @Query("SELECT m.id FROM Member m WHERE m.deletedAt < :cutoff")
    List<Long> findIdsWithdrawnBefore(@Param("cutoff") LocalDateTime cutoff);
}

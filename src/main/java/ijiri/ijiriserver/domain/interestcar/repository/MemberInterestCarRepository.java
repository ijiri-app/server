package ijiri.ijiriserver.domain.interestcar.repository;

import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberInterestCarRepository extends JpaRepository<MemberInterestCar, Long> {

    List<MemberInterestCar> findAllByMemberIdOrderByDisplayOrderAsc(Long memberId);

    // 전체 교체 시 INSERT 보다 먼저 즉시 실행되어야 unique 제약에 걸리지 않으므로 bulk delete 사용
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MemberInterestCar c WHERE c.memberId = :memberId")
    void deleteAllByMemberIdInBulk(@Param("memberId") Long memberId);
}

package ijiri.ijiriserver.domain.ownedcar.repository;

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnedCarRepository extends JpaRepository<OwnedCar, Long> {

    List<OwnedCar> findAllByMemberIdOrderByIdAsc(Long memberId);

    Optional<OwnedCar> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberId(Long memberId);

    @Modifying
    @Query("DELETE FROM OwnedCar c WHERE c.memberId = :memberId")
    void deleteAllByMemberIdInBulk(@Param("memberId") Long memberId);
}

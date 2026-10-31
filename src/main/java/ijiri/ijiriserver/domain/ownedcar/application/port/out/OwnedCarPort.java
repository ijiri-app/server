package ijiri.ijiriserver.domain.ownedcar.application.port.out;

import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus;

import java.util.List;
import java.util.Optional;

public interface OwnedCarPort {

    List<OwnedCar> findAllByMemberIdOrderByIdAsc(Long memberId);

    Optional<OwnedCar> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberIdAndStatus(Long memberId, OwnedCarStatus status);

    void deleteAllByMemberIdInBulk(Long memberId);

    void delete(OwnedCar entity);

    List<OwnedCar> findAllById(Iterable<Long> ids);

    Optional<OwnedCar> findById(Long id);

    OwnedCar save(OwnedCar entity);
}

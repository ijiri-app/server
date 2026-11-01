package ijiri.ijiriserver.domain.ownedcar.adapter.out.persistence;

import ijiri.ijiriserver.domain.ownedcar.application.port.out.OwnedCarPort;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OwnedCarPersistenceAdapter implements OwnedCarPort {

    private final OwnedCarJpaRepository ownedCarJpaRepository;

    @Override
    public List<OwnedCar> findAllByMemberIdOrderByIdAsc(Long memberId) {
        return ownedCarJpaRepository.findAllByMemberIdOrderByIdAsc(memberId);
    }

    @Override
    public Optional<OwnedCar> findByIdAndMemberId(Long id, Long memberId) {
        return ownedCarJpaRepository.findByIdAndMemberId(id, memberId);
    }

    @Override
    public long countByMemberIdAndStatus(Long memberId, OwnedCarStatus status) {
        return ownedCarJpaRepository.countByMemberIdAndStatus(memberId, status);
    }

    @Override
    public void deleteAllByMemberIdInBulk(Long memberId) {
        ownedCarJpaRepository.deleteAllByMemberIdInBulk(memberId);
    }

    @Override
    public void delete(OwnedCar entity) {
        ownedCarJpaRepository.delete(entity);
    }

    @Override
    public List<OwnedCar> findAllById(Iterable<Long> ids) {
        return ownedCarJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<OwnedCar> findById(Long id) {
        return ownedCarJpaRepository.findById(id);
    }

    @Override
    public OwnedCar save(OwnedCar entity) {
        return ownedCarJpaRepository.save(entity);
    }
}

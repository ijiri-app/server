package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence;

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarTrimPort;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarTrimPersistenceAdapter implements CarTrimPort {

    private final CarTrimJpaRepository carTrimJpaRepository;

    @Override
    public List<CarTrim> findAllByCarGenerationIdInOrderByIdAsc(Collection<Long> carGenerationIds) {
        return carTrimJpaRepository.findAllByCarGenerationIdInOrderByIdAsc(carGenerationIds);
    }

    @Override
    public boolean existsByCarGenerationIdAndName(Long carGenerationId, String name) {
        return carTrimJpaRepository.existsByCarGenerationIdAndName(carGenerationId, name);
    }

    @Override
    public Optional<CarTrim> findById(Long id) {
        return carTrimJpaRepository.findById(id);
    }

    @Override
    public CarTrim save(CarTrim entity) {
        return carTrimJpaRepository.save(entity);
    }
}

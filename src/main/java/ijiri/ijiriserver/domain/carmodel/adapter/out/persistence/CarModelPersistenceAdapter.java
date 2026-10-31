package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence;

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarModelPort;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarModelPersistenceAdapter implements CarModelPort {

    private final CarModelJpaRepository carModelJpaRepository;

    @Override
    public List<CarModel> search(String keyword, boolean coreOnly) {
        return carModelJpaRepository.search(keyword, coreOnly);
    }

    @Override
    public Optional<CarModel> findByIdAndHiddenFalse(Long id) {
        return carModelJpaRepository.findByIdAndHiddenFalse(id);
    }

    @Override
    public Optional<CarModel> findByBrandAndName(String brand, String name) {
        return carModelJpaRepository.findByBrandAndName(brand, name);
    }

    @Override
    public long countByIdInAndHiddenFalse(Collection<Long> ids) {
        return carModelJpaRepository.countByIdInAndHiddenFalse(ids);
    }

    @Override
    public long count() {
        return carModelJpaRepository.count();
    }

    @Override
    public List<CarModel> findAllById(Iterable<Long> ids) {
        return carModelJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<CarModel> findById(Long id) {
        return carModelJpaRepository.findById(id);
    }

    @Override
    public CarModel save(CarModel entity) {
        return carModelJpaRepository.save(entity);
    }
}

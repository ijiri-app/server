package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence;

import ijiri.ijiriserver.domain.carmodel.application.port.out.CarGenerationPort;
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CarGenerationPersistenceAdapter implements CarGenerationPort {

    private final CarGenerationJpaRepository carGenerationJpaRepository;

    @Override
    public List<CarGeneration> findAllByCarModelIdOrderByStartYearAscIdAsc(Long carModelId) {
        return carGenerationJpaRepository.findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId);
    }

    @Override
    public Optional<CarGeneration> findByCarModelIdAndCode(Long carModelId, String code) {
        return carGenerationJpaRepository.findByCarModelIdAndCode(carModelId, code);
    }

    @Override
    public Optional<CarGeneration> findById(Long id) {
        return carGenerationJpaRepository.findById(id);
    }

    @Override
    public CarGeneration save(CarGeneration entity) {
        return carGenerationJpaRepository.save(entity);
    }
}

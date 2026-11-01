package ijiri.ijiriserver.domain.carmodel.application.port.out;

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;

import java.util.List;
import java.util.Optional;

public interface CarGenerationPort {

    List<CarGeneration> findAllByCarModelIdOrderByStartYearAscIdAsc(Long carModelId);

    Optional<CarGeneration> findByCarModelIdAndCode(Long carModelId, String code);

    Optional<CarGeneration> findById(Long id);

    CarGeneration save(CarGeneration entity);
}

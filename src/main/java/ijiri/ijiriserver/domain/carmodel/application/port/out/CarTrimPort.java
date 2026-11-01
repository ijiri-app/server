package ijiri.ijiriserver.domain.carmodel.application.port.out;

import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CarTrimPort {

    List<CarTrim> findAllByCarGenerationIdInOrderByIdAsc(Collection<Long> carGenerationIds);

    boolean existsByCarGenerationIdAndName(Long carGenerationId, String name);

    Optional<CarTrim> findById(Long id);

    CarTrim save(CarTrim entity);
}

package ijiri.ijiriserver.domain.carmodel.application.port.out;

import ijiri.ijiriserver.domain.carmodel.entity.CarModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CarModelPort {

    List<CarModel> search(String keyword, boolean coreOnly);

    Optional<CarModel> findByIdAndHiddenFalse(Long id);

    Optional<CarModel> findByBrandAndName(String brand, String name);

    long countByIdInAndHiddenFalse(Collection<Long> ids);

    long count();

    List<CarModel> findAllById(Iterable<Long> ids);

    Optional<CarModel> findById(Long id);

    CarModel save(CarModel entity);
}

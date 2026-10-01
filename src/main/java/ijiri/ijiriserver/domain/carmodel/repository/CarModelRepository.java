package ijiri.ijiriserver.domain.carmodel.repository;

import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    List<CarModel> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<CarModel> findByManufacturerAndName(String manufacturer, String name);

    long countByIdIn(Collection<Long> ids);
}

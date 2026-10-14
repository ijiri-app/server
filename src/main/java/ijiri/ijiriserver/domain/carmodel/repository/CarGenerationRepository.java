package ijiri.ijiriserver.domain.carmodel.repository;

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarGenerationRepository extends JpaRepository<CarGeneration, Long> {

    List<CarGeneration> findAllByCarModelIdOrderByStartYearAscIdAsc(Long carModelId);

    Optional<CarGeneration> findByCarModelIdAndCode(Long carModelId, String code);
}

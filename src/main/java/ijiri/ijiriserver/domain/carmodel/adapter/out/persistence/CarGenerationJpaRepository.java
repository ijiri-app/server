package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence;

import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarGenerationJpaRepository extends JpaRepository<CarGeneration, Long> {

    List<CarGeneration> findAllByCarModelIdOrderByStartYearAscIdAsc(Long carModelId);

    Optional<CarGeneration> findByCarModelIdAndCode(Long carModelId, String code);
}

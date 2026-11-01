package ijiri.ijiriserver.domain.carmodel.adapter.out.persistence;

import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CarTrimJpaRepository extends JpaRepository<CarTrim, Long> {

    List<CarTrim> findAllByCarGenerationIdInOrderByIdAsc(Collection<Long> carGenerationIds);

    boolean existsByCarGenerationIdAndName(Long carGenerationId, String name);
}

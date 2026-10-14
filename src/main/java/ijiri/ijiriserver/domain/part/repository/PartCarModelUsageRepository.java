package ijiri.ijiriserver.domain.part.repository;

import ijiri.ijiriserver.domain.part.entity.PartCarModelUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartCarModelUsageRepository extends JpaRepository<PartCarModelUsage, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO part_car_model_usage (part_id, car_model_id, use_count)
            VALUES (:partId, :carModelId, GREATEST(:delta, 0))
            ON CONFLICT ON CONSTRAINT uk_part_car_model_usage_part_id_car_model_id
            DO UPDATE SET use_count = GREATEST(part_car_model_usage.use_count + :delta, 0)
            """, nativeQuery = true)
    void add(@Param("partId") Long partId, @Param("carModelId") Long carModelId, @Param("delta") int delta);
}

package ijiri.ijiriserver.car.part.adapter.out.persistence

import ijiri.ijiriserver.car.part.entity.PartCarModelUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PartCarModelUsageJpaRepository : JpaRepository<PartCarModelUsage, Long> {

    @Modifying
    @Query(
        value = """
        INSERT INTO part_car_model_usage (part_id, car_model_id, use_count)
        VALUES (:partId, :carModelId, GREATEST(:delta, 0))
        ON CONFLICT ON CONSTRAINT uk_part_car_model_usage_part_id_car_model_id
        DO UPDATE SET use_count = GREATEST(part_car_model_usage.use_count + :delta, 0)
        """,
        nativeQuery = true,
    )
    fun add(@Param("partId") partId: Long, @Param("carModelId") carModelId: Long, @Param("delta") delta: Int)
}

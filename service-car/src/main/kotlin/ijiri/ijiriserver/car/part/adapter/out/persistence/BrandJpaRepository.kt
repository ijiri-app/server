package ijiri.ijiriserver.car.part.adapter.out.persistence

import ijiri.ijiriserver.car.part.entity.Brand
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun findByNormalizedName(normalizedName: String): Brand?

    fun findByNormalizedNameContainingOrderByNameAsc(normalizedKeyword: String, limit: Limit): List<Brand>

    // 같은 브랜드를 동시에 새로 입력해도 예외 없이 한 줄만 남도록 충돌은 무시하고, 이어서 조회한다
    @Modifying
    @Query(
        value = """
        INSERT INTO brand (created_at, updated_at, name, normalized_name)
        VALUES (:now, :now, :name, :normalizedName)
        ON CONFLICT ON CONSTRAINT uk_brand_normalized_name DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("name") name: String,
        @Param("normalizedName") normalizedName: String,
        @Param("now") now: LocalDateTime,
    )
}

package ijiri.ijiriserver.domain.part.repository;

import ijiri.ijiriserver.domain.part.entity.Brand;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByNormalizedName(String normalizedName);

    List<Brand> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Limit limit);

    // 같은 브랜드를 동시에 새로 입력해도 예외 없이 한 줄만 남도록 충돌은 무시하고, 이어서 조회한다
    @Modifying
    @Query(value = """
            INSERT INTO brand (created_at, updated_at, name, normalized_name)
            VALUES (:now, :now, :name, :normalizedName)
            ON CONFLICT ON CONSTRAINT uk_brand_normalized_name DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("name") String name,
            @Param("normalizedName") String normalizedName,
            @Param("now") LocalDateTime now
    );
}

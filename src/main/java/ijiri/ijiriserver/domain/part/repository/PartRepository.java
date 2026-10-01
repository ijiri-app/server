package ijiri.ijiriserver.domain.part.repository;

import ijiri.ijiriserver.domain.part.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long>, JpaSpecificationExecutor<Part> {

    Optional<Part> findByBrandIdAndNormalizedName(Long brandId, String normalizedName);

    Optional<Part> findByBrandIdIsNullAndNormalizedName(String normalizedName);

    // 같은 부품을 동시에 새로 입력해도 예외 없이 한 줄만 남도록 충돌은 무시하고, 이어서 조회한다.
    // 네이티브 쿼리에 null 을 바인딩하면 타입을 알 수 없어 브랜드 없는 부품은 따로 넣는다
    @Modifying
    @Query(value = """
            INSERT INTO part (created_at, updated_at, brand_id, name, normalized_name, category)
            VALUES (:now, :now, :brandId, :name, :normalizedName, :category)
            ON CONFLICT ON CONSTRAINT uk_part_brand_id_normalized_name DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("brandId") Long brandId,
            @Param("name") String name,
            @Param("normalizedName") String normalizedName,
            @Param("category") String category,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query(value = """
            INSERT INTO part (created_at, updated_at, brand_id, name, normalized_name, category)
            VALUES (:now, :now, NULL, :name, :normalizedName, :category)
            ON CONFLICT ON CONSTRAINT uk_part_brand_id_normalized_name DO NOTHING
            """, nativeQuery = true)
    void insertWithoutBrandIfAbsent(
            @Param("name") String name,
            @Param("normalizedName") String normalizedName,
            @Param("category") String category,
            @Param("now") LocalDateTime now
    );

    // 부품명·브랜드명 부분 일치를 먼저, 그다음 부품명 trigram 유사도(오타 허용) 순.
    // category 가 빈 문자열이면 전체 분류 (네이티브 쿼리의 null 바인딩을 피한다)
    @Query(value = """
            SELECT p.* FROM part p
            LEFT JOIN brand b ON b.id = p.brand_id
            WHERE (:category = '' OR p.category = :category)
              AND (p.name ILIKE :pattern OR b.name ILIKE :pattern OR similarity(p.name, :keyword) > 0.3)
            ORDER BY (p.name ILIKE :pattern OR b.name ILIKE :pattern) DESC,
                     similarity(p.name, :keyword) DESC,
                     p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Part> suggest(
            @Param("keyword") String keyword,
            @Param("pattern") String pattern,
            @Param("category") String category,
            @Param("limit") int limit
    );
}

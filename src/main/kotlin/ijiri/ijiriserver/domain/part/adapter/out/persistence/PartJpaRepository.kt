package ijiri.ijiriserver.domain.part.adapter.out.persistence

import ijiri.ijiriserver.domain.part.application.data.PartSuggestion
import ijiri.ijiriserver.domain.part.entity.Part
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * 네이티브 쿼리에 null 을 바인딩하면 타입을 알 수 없어, 선택 조건은 빈 문자열·0 을 "조건 없음"으로 쓴다.
 * keyword, pattern 은 정규화(소문자, 공백·하이픈 제거)한 값이다.
 */
interface PartJpaRepository : JpaRepository<Part, Long> {

    fun findByBrandIdAndNormalizedName(brandId: Long, normalizedName: String): Part?

    fun findByBrandIdIsNullAndNormalizedName(normalizedName: String): Part?

    // 같은 부품을 동시에 새로 입력해도 예외 없이 한 줄만 남도록 충돌은 무시하고, 이어서 조회한다
    @Modifying
    @Query(
        value = """
        INSERT INTO part (created_at, updated_at, brand_id, name, normalized_name, category, status, use_count)
        VALUES (:now, :now, :brandId, :name, :normalizedName, :category, 'PENDING', 0)
        ON CONFLICT ON CONSTRAINT uk_part_brand_id_normalized_name DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("brandId") brandId: Long,
        @Param("name") name: String,
        @Param("normalizedName") normalizedName: String,
        @Param("category") category: String,
        @Param("now") now: LocalDateTime,
    )

    @Modifying
    @Query(
        value = """
        INSERT INTO part (created_at, updated_at, brand_id, name, normalized_name, category, status, use_count)
        VALUES (:now, :now, NULL, :name, :normalizedName, :category, 'PENDING', 0)
        ON CONFLICT ON CONSTRAINT uk_part_brand_id_normalized_name DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertWithoutBrandIfAbsent(
        @Param("name") name: String,
        @Param("normalizedName") normalizedName: String,
        @Param("category") category: String,
        @Param("now") now: LocalDateTime,
    )

    // 부품명, 브랜드명, 브랜드+부품명, 별칭의 부분 일치. 같은 차종에 자주 달린 부품, 전체 사용 수 순
    @Query(
        value = """
        SELECT p.* FROM part p
        LEFT JOIN brand b ON b.id = p.brand_id
        LEFT JOIN part_car_model_usage u ON u.part_id = p.id AND u.car_model_id = :carModelId
        WHERE (:category = '' OR p.category = :category)
          AND (:pattern = '%%'
               OR p.normalized_name LIKE :pattern
               OR b.normalized_name LIKE :pattern
               OR COALESCE(b.normalized_name, '') || p.normalized_name LIKE :pattern
               OR EXISTS (SELECT 1 FROM part_alias a WHERE a.part_id = p.id AND a.normalized_name LIKE :pattern))
        ORDER BY COALESCE(u.use_count, 0) DESC, p.use_count DESC, p.id DESC
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun search(
        @Param("pattern") pattern: String,
        @Param("category") category: String,
        @Param("carModelId") carModelId: Long,
        @Param("limit") limit: Int,
    ): List<Part>

    // "혹시 이 부품인가요?" 후보. 부분 일치는 1, 그 밖에는 브랜드+부품명·별칭과의 trigram 유사도.
    // 같은 점수면 같은 차종에 자주 달린 부품 순
    @Query(
        value = """
        SELECT p.id AS id,
               CASE WHEN COALESCE(b.normalized_name, '') || p.normalized_name LIKE :pattern
                         OR p.normalized_name LIKE :pattern THEN 1.0
                    ELSE GREATEST(
                        word_similarity(:keyword, COALESCE(b.normalized_name, '') || p.normalized_name),
                        COALESCE((SELECT MAX(word_similarity(:keyword, a.normalized_name))
                                  FROM part_alias a WHERE a.part_id = p.id), 0))
               END AS score
        FROM part p
        LEFT JOIN brand b ON b.id = p.brand_id
        LEFT JOIN part_car_model_usage u ON u.part_id = p.id AND u.car_model_id = :carModelId
        WHERE (:category = '' OR p.category = :category)
        ORDER BY score DESC, COALESCE(u.use_count, 0) DESC, p.use_count DESC
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun suggest(
        @Param("keyword") keyword: String,
        @Param("pattern") pattern: String,
        @Param("category") category: String,
        @Param("carModelId") carModelId: Long,
        @Param("limit") limit: Int,
    ): List<PartSuggestion>

    @Modifying
    @Query(
        "UPDATE Part p SET p.useCount = CASE WHEN p.useCount + :delta < 0 THEN 0 ELSE p.useCount + :delta END " +
            "WHERE p.id IN :ids",
    )
    fun addUseCount(@Param("ids") ids: Collection<Long>, @Param("delta") delta: Int)
}

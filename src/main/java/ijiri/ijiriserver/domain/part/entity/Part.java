package ijiri.ijiriserver.domain.part.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부품. 같은 브랜드(브랜드 없음 포함) 안에서 normalizedName(대소문자·공백·하이픈 무시)이 같으면 같은 부품으로 본다.
 * 그 밖의 비슷한 이름은 자동으로 합치지 않는다 (TE37 SAGA 18" 과 TE37 SONIC 19" 는 다른 부품).
 * 분류는 처음 등록할 때 정한다. useCount 는 이 부품이 달린 게시물 수.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "part",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_part_brand_id_normalized_name",
                columnNames = {"brand_id", "normalized_name"}
        ),
        // 마이그레이션에서 trigram(GIN) 인덱스로 만든다
        indexes = @Index(name = "idx_part_normalized_name", columnList = "normalized_name")
)
public class Part extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null 이면 브랜드 없음(자작 등)
    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PartCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PartStatus status;

    @Column(name = "use_count", nullable = false)
    private int useCount;
}

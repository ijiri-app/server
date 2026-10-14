package ijiri.ijiriserver.domain.part.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 부품 브랜드. 사용자가 게시물을 올리며 새로 입력할 수 있다.
 * normalizedName(대소문자·공백·하이픈 무시)이 같으면 같은 브랜드로 본다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "brand",
        uniqueConstraints = @UniqueConstraint(name = "uk_brand_normalized_name", columnNames = "normalized_name"),
        // 마이그레이션에서 trigram(GIN) 인덱스로 만든다
        indexes = @Index(name = "idx_brand_normalized_name", columnList = "normalized_name")
)
public class Brand extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;
}

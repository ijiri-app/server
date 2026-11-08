package ijiri.ijiriserver.car.part.entity

import ijiri.ijiriserver.common.contract.PartCategory
import ijiri.ijiriserver.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 부품. 같은 브랜드(브랜드 없음 포함) 안에서 normalizedName(대소문자·공백·하이픈 무시)이 같으면 같은 부품으로 본다.
 * 그 밖의 비슷한 이름은 자동으로 합치지 않는다 (TE37 SAGA 18" 과 TE37 SONIC 19" 는 다른 부품).
 * 분류는 처음 등록할 때 정한다. useCount 는 이 부품이 달린 게시물 수.
 */
@Entity
@Table(
    name = "part",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_part_brand_id_normalized_name",
            columnNames = ["brand_id", "normalized_name"],
        ),
    ],
    // 마이그레이션에서 trigram(GIN) 인덱스로 만든다
    indexes = [Index(name = "idx_part_normalized_name", columnList = "normalized_name")],
)
class Part(
    brandId: Long? = null,
    name: String,
    normalizedName: String,
    category: PartCategory,
    status: PartStatus,
    useCount: Int = 0,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    // null 이면 브랜드 없음(자작 등)
    @Column(name = "brand_id")
    var brandId: Long? = brandId
        protected set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(name = "normalized_name", nullable = false, length = 100)
    var normalizedName: String = normalizedName
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: PartCategory = category
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PartStatus = status
        protected set

    @Column(name = "use_count", nullable = false)
    var useCount: Int = useCount
        protected set
}

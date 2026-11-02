package ijiri.ijiriserver.car.part.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 부품의 다른 이름. 관리자가 부품을 합치면 사라진 쪽 이름이 별칭으로 남고, 부품 검색이 별칭도 찾는다.
 */
@Entity
@Table(
    name = "part_alias",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_part_alias_part_id_normalized_name",
            columnNames = ["part_id", "normalized_name"],
        ),
    ],
    // 마이그레이션에서 trigram(GIN) 인덱스로 만든다
    indexes = [Index(name = "idx_part_alias_normalized_name", columnList = "normalized_name")],
)
class PartAlias(
    partId: Long,
    name: String,
    normalizedName: String,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "part_id", nullable = false)
    var partId: Long = partId
        protected set

    @Column(name = "name", nullable = false, length = 150)
    var name: String = name
        protected set

    @Column(name = "normalized_name", nullable = false, length = 150)
    var normalizedName: String = normalizedName
        protected set
}

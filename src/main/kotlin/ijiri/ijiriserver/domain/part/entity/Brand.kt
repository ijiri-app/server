package ijiri.ijiriserver.domain.part.entity

import ijiri.ijiriserver.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 부품 브랜드. 사용자가 게시물을 올리며 새로 입력할 수 있다.
 * normalizedName(대소문자·공백·하이픈 무시)이 같으면 같은 브랜드로 본다.
 */
@Entity
@Table(
    name = "brand",
    uniqueConstraints = [UniqueConstraint(name = "uk_brand_normalized_name", columnNames = ["normalized_name"])],
    // 마이그레이션에서 trigram(GIN) 인덱스로 만든다
    indexes = [Index(name = "idx_brand_normalized_name", columnList = "normalized_name")],
)
class Brand(
    name: String,
    normalizedName: String,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "normalized_name", nullable = false, length = 50)
    var normalizedName: String = normalizedName
        protected set
}

package ijiri.ijiriserver.domain.carmodel.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 차종 마스터의 1단계(모델). 브랜드가 판매하는 이름 기준이라 아반떼 N 과 아반떼 N 라인, M3 와 3시리즈는 별개다.
 * 관심 차종과 피드 탭이 이 단위로 묶인다. isCore 는 온보딩에서 위에 보여줄 핵심 계열.
 */
@Entity
@Table(
    name = "car_model",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_car_model_brand_name",
            columnNames = ["brand", "name"],
        ),
    ],
)
class CarModel(
    brand: String,
    name: String,
    core: Boolean = false,
    imageUrl: String? = null,
    hidden: Boolean = false,
    displayOrder: Int = 0,
    id: Long = 0L,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "brand", nullable = false, length = 50)
    var brand: String = brand
        protected set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(name = "is_core", nullable = false)
    var core: Boolean = core
        protected set

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = imageUrl
        protected set

    // 숨긴 차종은 목록·검색에 나오지 않는다 (이미 연결된 게시물·보유 차량은 그대로)
    @Column(name = "hidden", nullable = false)
    var hidden: Boolean = hidden
        protected set

    // 목록에 보이는 순서 (CSV 에 처음 나온 순서)
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        protected set
}

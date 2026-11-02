package ijiri.ijiriserver.domain.interestcar.entity

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
 * 회원의 관심 차종. displayOrder 는 앱 화면에 보이는 순서(0부터).
 */
@Entity
@Table(
    name = "member_interest_car",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_interest_car_member_id_car_model_id",
            columnNames = ["member_id", "car_model_id"],
        ),
    ],
    indexes = [Index(name = "idx_member_interest_car_member_id", columnList = "member_id")],
)
class MemberInterestCar(
    memberId: Long,
    carModelId: Long,
    displayOrder: Int,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "car_model_id", nullable = false, updatable = false)
    var carModelId: Long = carModelId
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        protected set
}

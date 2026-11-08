package ijiri.ijiriserver.user.ownedcar.entity

import ijiri.ijiriserver.common.contract.BuildStyle
import ijiri.ijiriserver.common.contract.CarSpec
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

/**
 * 회원의 보유 차량. 트림을 고르면 모델·세대가 정해지고, 게시물을 올릴 때 이 중 하나를 고른다.
 * 트림은 바꿀 수 없다 (게시물이 이 차량을 가리키므로).
 */
@Entity
@Table(
    name = "owned_car",
    indexes = [Index(name = "idx_owned_car_member_id", columnList = "member_id")],
)
class OwnedCar(
    memberId: Long,
    carModelId: Long,
    carGenerationId: Long,
    carTrimId: Long,
    modelYear: Int,
    buildStyle: BuildStyle? = null,
    nickname: String? = null,
    status: OwnedCarStatus,
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

    @Column(name = "car_generation_id", nullable = false, updatable = false)
    var carGenerationId: Long = carGenerationId
        protected set

    @Column(name = "car_trim_id", nullable = false, updatable = false)
    var carTrimId: Long = carTrimId
        protected set

    @Column(name = "model_year", nullable = false)
    var modelYear: Int = modelYear
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "build_style", length = 20)
    var buildStyle: BuildStyle? = buildStyle
        protected set

    @Column(name = "nickname", length = 20)
    var nickname: String? = nickname
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OwnedCarStatus = status
        protected set

    fun changeModelYear(modelYear: Int) {
        this.modelYear = modelYear
    }

    fun changeBuildStyle(buildStyle: BuildStyle) {
        this.buildStyle = buildStyle
    }

    fun changeNickname(nickname: String?) {
        this.nickname = nickname
    }

    fun changeStatus(status: OwnedCarStatus) {
        this.status = status
    }

    companion object {
        fun of(memberId: Long, spec: CarSpec, modelYear: Int, buildStyle: BuildStyle?, nickname: String?): OwnedCar {
            return OwnedCar(
                memberId = memberId,
                carModelId = spec.carModelId,
                carGenerationId = spec.generationId,
                carTrimId = spec.trimId,
                modelYear = modelYear,
                buildStyle = buildStyle,
                nickname = nickname,
                status = OwnedCarStatus.OWNED,
            )
        }
    }
}

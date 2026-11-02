package ijiri.ijiriserver.user.ownedcar.application.service

import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.common.contract.OwnedCarSnapshot
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import ijiri.ijiriserver.user.ownedcar.application.data.request.OwnedCarCreateRequest
import ijiri.ijiriserver.user.ownedcar.application.data.request.OwnedCarUpdateRequest
import ijiri.ijiriserver.user.ownedcar.application.data.response.OwnedCarResponse
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.CreateOwnedCarUseCase
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.DeleteOwnedCarUseCase
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.GetOwnedCarSnapshotUseCase
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.GetOwnedCarsUseCase
import ijiri.ijiriserver.user.ownedcar.application.port.`in`.UpdateOwnedCarUseCase
import ijiri.ijiriserver.user.ownedcar.application.port.out.CarSpecReader
import ijiri.ijiriserver.user.ownedcar.application.port.out.OwnedCarPort
import ijiri.ijiriserver.user.ownedcar.application.port.out.OwnedCarPostCounter
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus
import ijiri.ijiriserver.user.ownedcar.exception.OwnedCarStatusCode
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class OwnedCarService(
    private val ownedCarPort: OwnedCarPort,
    private val carSpecReader: CarSpecReader,
    private val getMemberUseCase: GetMemberUseCase,
    private val ownedCarPostCounter: OwnedCarPostCounter,
    private val clock: Clock,
) : GetOwnedCarsUseCase,
    CreateOwnedCarUseCase,
    UpdateOwnedCarUseCase,
    DeleteOwnedCarUseCase,
    GetOwnedCarSnapshotUseCase {

    // 지금 타는 차를 먼저, 그다음 이전 차량. 각각 등록순
    override fun getAll(memberId: Long): OwnedCarResponse {
        val cars = ownedCarPort.findAllByMemberIdOrderByIdAsc(memberId)
        val specs = carSpecReader.getSpecsByTrims(cars.map(OwnedCar::carTrimId).toSet())
        val postCounts = ownedCarPostCounter.countPosts(cars.map(OwnedCar::id))
        return OwnedCarResponse.list(
            cars.sortedBy(OwnedCar::status)
                .map { car ->
                    OwnedCarResponse.Item.of(
                        car,
                        specs.getValue(car.carTrimId),
                        postCounts[car.id] ?: 0L,
                    )
                },
        )
    }

    // 탈퇴 후 만료 전 access token 으로 들어온 요청이 데이터를 다시 만들지 않도록 활성 회원만 허용
    @Transactional
    override fun create(memberId: Long, request: OwnedCarCreateRequest): OwnedCarResponse {
        getMemberUseCase.getById(memberId)
        // 게시물이 있어 지우지 못하고 이전 차량(PAST)으로 남은 차는 세지 않는다
        if (ownedCarPort.countByMemberIdAndStatus(memberId, OwnedCarStatus.OWNED) >= MAX_OWNED_CARS) {
            throw CustomException(OwnedCarStatusCode.OWNED_CAR_LIMIT_EXCEEDED)
        }
        val spec = carSpecReader.getSpecByTrim(request.trimId!!)
        validateModelYear(spec, request.year!!)
        val car = ownedCarPort.save(
            OwnedCar.of(
                memberId,
                spec,
                request.year,
                request.buildStyle,
                request.nickname,
            ),
        )
        return OwnedCarResponse.created(car.id)
    }

    @Transactional
    override fun update(memberId: Long, ownedCarId: Long, request: OwnedCarUpdateRequest) {
        val car = getOwnedCar(memberId, ownedCarId)
        if (request.year != null) {
            validateModelYear(carSpecReader.getSpecByTrim(car.carTrimId), request.year)
            car.changeModelYear(request.year)
        }
        if (request.buildStyle != null) {
            car.changeBuildStyle(request.buildStyle)
        }
        if (request.nickname != null) {
            car.changeNickname(request.nickname.trim().ifEmpty { null })
        }
        if (request.status != null) {
            car.changeStatus(request.status)
        }
    }

    @Transactional
    override fun delete(memberId: Long, ownedCarId: Long) {
        val car = getOwnedCar(memberId, ownedCarId)
        if ((ownedCarPostCounter.countPosts(listOf(ownedCarId))[ownedCarId] ?: 0L) > 0) {
            car.changeStatus(OwnedCarStatus.PAST)
            return
        }
        ownedCarPort.delete(car)
    }

    override fun getSnapshot(memberId: Long, ownedCarId: Long): OwnedCarSnapshot {
        val car = getOwnedCar(memberId, ownedCarId)
        return OwnedCarSnapshot(car.id, car.memberId, carSpecReader.getSpecByTrim(car.carTrimId), car.modelYear)
    }

    // 영구 삭제. 게시물은 service-community 가 Kafka 로 받아 따로 지운다
    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        ownedCarPort.deleteAllByMemberIdInBulk(event.memberId)
    }

    // 연식은 세대의 판매 기간 안에서만 (판매 중이면 올해 + 1 까지)
    private fun validateModelYear(spec: CarSpec, year: Int) {
        if (!spec.coversYear(year, LocalDate.now(clock).year)) {
            throw CustomException(OwnedCarStatusCode.INVALID_MODEL_YEAR)
        }
    }

    private fun getOwnedCar(memberId: Long, ownedCarId: Long): OwnedCar {
        val car = ownedCarPort.findById(ownedCarId)
            ?: throw CustomException(OwnedCarStatusCode.OWNED_CAR_NOT_FOUND)
        if (car.memberId != memberId) {
            throw CustomException(OwnedCarStatusCode.NOT_OWNER)
        }
        return car
    }

    companion object {
        private const val MAX_OWNED_CARS = 10
    }
}

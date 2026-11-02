package ijiri.ijiriserver.domain.ownedcar.application.service

import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarSpecByTrimUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.ValidateModelYearUseCase
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarCreateRequest
import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarUpdateRequest
import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.CreateOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.DeleteOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.GetOwnedCarSnapshotUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.GetOwnedCarSnapshotsUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.GetOwnedCarsUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.`in`.UpdateOwnedCarUseCase
import ijiri.ijiriserver.domain.ownedcar.application.port.out.OwnedCarPort
import ijiri.ijiriserver.domain.ownedcar.application.port.out.OwnedCarPostCounter
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarSnapshot
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OwnedCarService(
    private val ownedCarPort: OwnedCarPort,
    private val getCarSpecByTrimUseCase: GetCarSpecByTrimUseCase,
    private val validateModelYearUseCase: ValidateModelYearUseCase,
    private val getMemberUseCase: GetMemberUseCase,
    private val ownedCarPostCounter: OwnedCarPostCounter,
) : GetOwnedCarsUseCase,
    CreateOwnedCarUseCase,
    UpdateOwnedCarUseCase,
    DeleteOwnedCarUseCase,
    GetOwnedCarSnapshotUseCase,
    GetOwnedCarSnapshotsUseCase {

    // 지금 타는 차를 먼저, 그다음 이전 차량. 각각 등록순
    override fun getAll(memberId: Long): OwnedCarResponse {
        val cars = ownedCarPort.findAllByMemberIdOrderByIdAsc(memberId)
        val postCounts = ownedCarPostCounter.countPosts(cars.map(OwnedCar::id))
        return OwnedCarResponse.list(
            cars.sortedBy(OwnedCar::status)
                .map { car ->
                    OwnedCarResponse.Item.of(
                        car,
                        getCarSpecByTrimUseCase.getSpecByTrim(car.carTrimId),
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
        val spec = getCarSpecByTrimUseCase.getSpecByTrim(request.trimId!!)
        validateModelYearUseCase.validateModelYear(spec.generationId, request.year!!)
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
            validateModelYearUseCase.validateModelYear(car.carGenerationId, request.year)
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
        return toSnapshot(getOwnedCar(memberId, ownedCarId))
    }

    override fun getSnapshots(ownedCarIds: Collection<Long>): Map<Long, OwnedCarSnapshot> {
        return ownedCarPort.findAllById(ownedCarIds).associate { car -> car.id to toSnapshot(car) }
    }

    // 게시물을 먼저 지운 뒤에 실행되어야 한다 (영구 삭제 순서: 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원)
    @Order(3)
    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        ownedCarPort.deleteAllByMemberIdInBulk(event.memberId)
    }

    private fun getOwnedCar(memberId: Long, ownedCarId: Long): OwnedCar {
        val car = ownedCarPort.findById(ownedCarId)
            ?: throw CustomException(OwnedCarStatusCode.OWNED_CAR_NOT_FOUND)
        if (car.memberId != memberId) {
            throw CustomException(OwnedCarStatusCode.NOT_OWNER)
        }
        return car
    }

    private fun toSnapshot(car: OwnedCar): OwnedCarSnapshot {
        return OwnedCarSnapshot(
            car.id,
            getCarSpecByTrimUseCase.getSpecByTrim(car.carTrimId),
            car.modelYear,
        )
    }

    companion object {
        private const val MAX_OWNED_CARS = 10
    }
}

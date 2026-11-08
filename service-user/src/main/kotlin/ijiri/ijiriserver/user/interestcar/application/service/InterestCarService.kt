package ijiri.ijiriserver.user.interestcar.application.service

import ijiri.ijiriserver.common.contract.CarModelInfo
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.user.interestcar.application.data.response.InterestCarResponse
import ijiri.ijiriserver.user.interestcar.application.port.`in`.GetInterestCarModelIdsUseCase
import ijiri.ijiriserver.user.interestcar.application.port.`in`.GetInterestCarsUseCase
import ijiri.ijiriserver.user.interestcar.application.port.`in`.ReplaceInterestCarsUseCase
import ijiri.ijiriserver.user.interestcar.application.port.out.CarModelReader
import ijiri.ijiriserver.user.interestcar.application.port.out.MemberInterestCarPort
import ijiri.ijiriserver.user.interestcar.entity.MemberInterestCar
import ijiri.ijiriserver.user.interestcar.exception.InterestCarStatusCode
import ijiri.ijiriserver.user.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InterestCarService(
    private val memberInterestCarPort: MemberInterestCarPort,
    private val getMemberUseCase: GetMemberUseCase,
    private val carModelReader: CarModelReader,
) : GetInterestCarsUseCase,
    ReplaceInterestCarsUseCase,
    GetInterestCarModelIdsUseCase {

    // 화면 순서(displayOrder)대로. 차종 마스터에서 사라진 id 는 빠진다
    override fun getAll(memberId: Long): InterestCarResponse {
        val carModelIds = getCarModelIds(memberId)
        val infos = carModelReader.getCarModelInfos(carModelIds).associateBy(CarModelInfo::id)
        return InterestCarResponse.from(carModelIds.mapNotNull { infos[it] })
    }

    @Transactional
    override fun replaceAll(memberId: Long, carModelIds: List<Long>) {
        // 탈퇴 후 만료 전 access token 으로 들어온 요청이 데이터를 다시 만들지 않도록 활성 회원만 허용
        getMemberUseCase.getById(memberId)
        if (carModelIds.toHashSet().size != carModelIds.size) {
            throw CustomException(InterestCarStatusCode.DUPLICATE_CAR_MODEL)
        }
        // 관리자가 숨긴 차종이라도 이미 고른 것은 남겨 둘 수 있게, 새로 추가하는 차종만 확인한다
        val current = getCarModelIds(memberId)
        validateCarModelsExist(carModelIds.filter { carModelId -> !current.contains(carModelId) })

        memberInterestCarPort.deleteAllByMemberIdInBulk(memberId)
        memberInterestCarPort.saveAll(
            carModelIds.mapIndexed { order, carModelId ->
                MemberInterestCar(
                    memberId = memberId,
                    carModelId = carModelId,
                    displayOrder = order,
                )
            },
        )
    }

    override fun getCarModelIds(memberId: Long): List<Long> {
        return memberInterestCarPort.findAllByMemberIdOrderByDisplayOrderAsc(memberId)
            .map { it.carModelId }
    }

    @EventListener
    @Transactional
    fun removeAll(event: MemberPurgedEvent) {
        memberInterestCarPort.deleteAllByMemberIdInBulk(event.memberId)
    }

    // 모든 id 가 숨기지 않은 차종이어야 한다
    private fun validateCarModelsExist(carModelIds: Collection<Long>) {
        if (carModelIds.isEmpty()) {
            return
        }
        val found = carModelReader.getCarModelInfos(carModelIds)
        if (found.size != carModelIds.toSet().size || found.any(CarModelInfo::hidden)) {
            throw CustomException(InterestCarStatusCode.CAR_MODEL_NOT_FOUND)
        }
    }
}

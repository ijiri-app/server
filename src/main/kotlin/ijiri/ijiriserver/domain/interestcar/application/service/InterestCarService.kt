package ijiri.ijiriserver.domain.interestcar.application.service

import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelInfosUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.ValidateCarModelsExistUseCase
import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.GetInterestCarModelIdsUseCase
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.GetInterestCarsUseCase
import ijiri.ijiriserver.domain.interestcar.application.port.`in`.ReplaceInterestCarsUseCase
import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar
import ijiri.ijiriserver.domain.interestcar.exception.InterestCarStatusCode
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InterestCarService(
    private val memberInterestCarPort: MemberInterestCarPort,
    private val getMemberUseCase: GetMemberUseCase,
    private val getCarModelInfosUseCase: GetCarModelInfosUseCase,
    private val validateCarModelsExistUseCase: ValidateCarModelsExistUseCase,
) : GetInterestCarsUseCase,
    ReplaceInterestCarsUseCase,
    GetInterestCarModelIdsUseCase {

    override fun getAll(memberId: Long): InterestCarResponse {
        return InterestCarResponse.from(getCarModelInfosUseCase.getCarModelInfos(getCarModelIds(memberId)))
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
        validateCarModelsExistUseCase.validateCarModelsExist(
            carModelIds.filter { carModelId -> !current.contains(carModelId) },
        )

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
}

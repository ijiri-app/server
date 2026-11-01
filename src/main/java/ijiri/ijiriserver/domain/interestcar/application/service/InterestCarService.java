package ijiri.ijiriserver.domain.interestcar.application.service;

import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarModelInfosUseCase;
import ijiri.ijiriserver.domain.carmodel.application.port.in.ValidateCarModelsExistUseCase;
import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse;
import ijiri.ijiriserver.domain.interestcar.application.port.in.GetAllInterestCarUseCase;
import ijiri.ijiriserver.domain.interestcar.application.port.in.GetCarModelIdsInterestCarUseCase;
import ijiri.ijiriserver.domain.interestcar.application.port.in.ReplaceAllInterestCarUseCase;
import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort;
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;
import ijiri.ijiriserver.domain.interestcar.exception.InterestCarStatusCode;
import ijiri.ijiriserver.domain.member.application.port.in.GetByIdMemberUseCase;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestCarService implements GetAllInterestCarUseCase,
        ReplaceAllInterestCarUseCase,
        GetCarModelIdsInterestCarUseCase {

    private final MemberInterestCarPort memberInterestCarPort;
    private final GetByIdMemberUseCase getByIdMemberUseCase;
    private final GetCarModelInfosUseCase getCarModelInfosUseCase;
    private final ValidateCarModelsExistUseCase validateCarModelsExistUseCase;

    @Override
    public InterestCarResponse getAll(Long memberId) {
        return InterestCarResponse.from(getCarModelInfosUseCase.getCarModelInfos(getCarModelIds(memberId)));
    }

    @Override
    @Transactional
    public void replaceAll(Long memberId, List<Long> carModelIds) {
        // 탈퇴 후 만료 전 access token 으로 들어온 요청이 데이터를 다시 만들지 않도록 활성 회원만 허용
        getByIdMemberUseCase.getById(memberId);
        if (new HashSet<>(carModelIds).size() != carModelIds.size()) {
            throw new CustomException(InterestCarStatusCode.DUPLICATE_CAR_MODEL);
        }
        // 관리자가 숨긴 차종이라도 이미 고른 것은 남겨 둘 수 있게, 새로 추가하는 차종만 확인한다
        List<Long> current = getCarModelIds(memberId);
        validateCarModelsExistUseCase.validateCarModelsExist(carModelIds.stream()
                .filter(carModelId -> !current.contains(carModelId))
                .toList()
        );

        memberInterestCarPort.deleteAllByMemberIdInBulk(memberId);
        memberInterestCarPort.saveAll(IntStream.range(0, carModelIds.size())
                .mapToObj(order -> MemberInterestCar.builder()
                        .memberId(memberId)
                        .carModelId(carModelIds.get(order))
                        .displayOrder(order)
                        .build()
                )
                .toList()
        );
    }

    @Override
    public List<Long> getCarModelIds(Long memberId) {
        return memberInterestCarPort.findAllByMemberIdOrderByDisplayOrderAsc(memberId).stream()
                .map(MemberInterestCar::getCarModelId)
                .toList();
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        memberInterestCarPort.deleteAllByMemberIdInBulk(event.memberId());
    }
}

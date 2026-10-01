package ijiri.ijiriserver.domain.ownedcar.service.impl;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.service.CarModelService;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.domain.ownedcar.dto.OwnedCarSnapshot;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.request.OwnedCarUpdateRequest;
import ijiri.ijiriserver.domain.ownedcar.dto.response.OwnedCarResponse;
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCar;
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode;
import ijiri.ijiriserver.domain.ownedcar.repository.OwnedCarRepository;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnedCarServiceImpl implements OwnedCarService {

    private static final int MAX_OWNED_CARS = 10;

    private final OwnedCarRepository ownedCarRepository;
    private final CarModelService carModelService;
    private final MemberService memberService;

    @Override
    public OwnedCarResponse getAll(Long memberId) {
        return OwnedCarResponse.list(ownedCarRepository.findAllByMemberIdOrderByIdAsc(memberId).stream()
                .map(car -> OwnedCarResponse.Car.of(car, specOf(car)))
                .toList()
        );
    }

    // 탈퇴 후 만료 전 access token 으로 들어온 요청이 데이터를 다시 만들지 않도록 활성 회원만 허용
    @Override
    @Transactional
    public OwnedCarResponse create(Long memberId, OwnedCarCreateRequest request) {
        memberService.getById(memberId);
        if (ownedCarRepository.countByMemberId(memberId) >= MAX_OWNED_CARS) {
            throw new CustomException(OwnedCarStatusCode.OWNED_CAR_LIMIT_EXCEEDED);
        }
        CarSpec spec = carModelService.getSpec(
                request.carModelId(),
                request.carGenerationId(),
                request.carTrimId()
        );
        OwnedCar car = ownedCarRepository.save(OwnedCar.of(
                memberId,
                spec,
                request.modelYear(),
                request.buildDirection()
        ));
        return OwnedCarResponse.single(OwnedCarResponse.Car.of(car, spec));
    }

    @Override
    @Transactional
    public OwnedCarResponse update(Long memberId, Long ownedCarId, OwnedCarUpdateRequest request) {
        OwnedCar car = getOwnedCar(memberId, ownedCarId);
        CarSpec spec = carModelService.getSpec(
                request.carModelId(),
                request.carGenerationId(),
                request.carTrimId()
        );
        car.update(spec, request.modelYear(), request.buildDirection());
        return OwnedCarResponse.single(OwnedCarResponse.Car.of(car, spec));
    }

    // 게시물은 차량 정보를 복사해 두므로 보유 차량을 지워도 게시물에는 영향이 없다
    @Override
    @Transactional
    public OwnedCarResponse delete(Long memberId, Long ownedCarId) {
        ownedCarRepository.delete(getOwnedCar(memberId, ownedCarId));
        return OwnedCarResponse.message(OwnedCarStatusCode.DELETE_SUCCESS.getMessage());
    }

    @Override
    public OwnedCarSnapshot getSnapshot(Long memberId, Long ownedCarId) {
        OwnedCar car = getOwnedCar(memberId, ownedCarId);
        return new OwnedCarSnapshot(specOf(car), car.getModelYear(), car.getBuildDirection());
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        ownedCarRepository.deleteAllByMemberIdInBulk(event.memberId());
    }

    private OwnedCar getOwnedCar(Long memberId, Long ownedCarId) {
        return ownedCarRepository.findByIdAndMemberId(ownedCarId, memberId)
                .orElseThrow(() -> new CustomException(OwnedCarStatusCode.OWNED_CAR_NOT_FOUND));
    }

    private CarSpec specOf(OwnedCar car) {
        return carModelService.getSpec(car.getCarModelId(), car.getCarGenerationId(), car.getCarTrimId());
    }
}

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
import ijiri.ijiriserver.domain.ownedcar.entity.OwnedCarStatus;
import ijiri.ijiriserver.domain.ownedcar.exception.OwnedCarStatusCode;
import ijiri.ijiriserver.domain.ownedcar.repository.OwnedCarRepository;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarPostCounter;
import ijiri.ijiriserver.domain.ownedcar.service.OwnedCarService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnedCarServiceImpl implements OwnedCarService {

    private static final int MAX_OWNED_CARS = 10;

    private final OwnedCarRepository ownedCarRepository;
    private final CarModelService carModelService;
    private final MemberService memberService;
    private final OwnedCarPostCounter ownedCarPostCounter;

    // 지금 타는 차를 먼저, 그다음 이전 차량. 각각 등록순
    @Override
    public OwnedCarResponse getAll(Long memberId) {
        List<OwnedCar> cars = ownedCarRepository.findAllByMemberIdOrderByIdAsc(memberId);
        Map<Long, Long> postCounts = ownedCarPostCounter.countPosts(cars.stream().map(OwnedCar::getId).toList());
        return OwnedCarResponse.list(cars.stream()
                .sorted((a, b) -> a.getStatus().compareTo(b.getStatus()))
                .map(car -> OwnedCarResponse.Item.of(
                        car,
                        carModelService.getSpecByTrim(car.getCarTrimId()),
                        postCounts.getOrDefault(car.getId(), 0L)
                ))
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
        CarSpec spec = carModelService.getSpecByTrim(request.trimId());
        carModelService.validateModelYear(spec.generationId(), request.year());
        OwnedCar car = ownedCarRepository.save(OwnedCar.of(
                memberId,
                spec,
                request.year(),
                request.buildStyle(),
                request.nickname()
        ));
        return OwnedCarResponse.created(car.getId());
    }

    @Override
    @Transactional
    public void update(Long memberId, Long ownedCarId, OwnedCarUpdateRequest request) {
        OwnedCar car = getOwnedCar(memberId, ownedCarId);
        if (request.year() != null) {
            carModelService.validateModelYear(car.getCarGenerationId(), request.year());
            car.changeModelYear(request.year());
        }
        if (request.buildStyle() != null) {
            car.changeBuildStyle(request.buildStyle());
        }
        if (request.nickname() != null) {
            car.changeNickname(request.nickname().isBlank() ? null : request.nickname().strip());
        }
        if (request.status() != null) {
            car.changeStatus(request.status());
        }
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long ownedCarId) {
        OwnedCar car = getOwnedCar(memberId, ownedCarId);
        if (ownedCarPostCounter.countPosts(List.of(ownedCarId)).getOrDefault(ownedCarId, 0L) > 0) {
            car.changeStatus(OwnedCarStatus.PAST);
            return;
        }
        ownedCarRepository.delete(car);
    }

    @Override
    public OwnedCarSnapshot getSnapshot(Long memberId, Long ownedCarId) {
        return toSnapshot(getOwnedCar(memberId, ownedCarId));
    }

    @Override
    public Map<Long, OwnedCarSnapshot> getSnapshots(Collection<Long> ownedCarIds) {
        return ownedCarRepository.findAllById(ownedCarIds).stream()
                .collect(Collectors.toMap(OwnedCar::getId, this::toSnapshot));
    }

    // 게시물을 먼저 지운 뒤에 실행되어야 한다 (영구 삭제 순서: 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원)
    @Order(3)
    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        ownedCarRepository.deleteAllByMemberIdInBulk(event.memberId());
    }

    private OwnedCar getOwnedCar(Long memberId, Long ownedCarId) {
        OwnedCar car = ownedCarRepository.findById(ownedCarId)
                .orElseThrow(() -> new CustomException(OwnedCarStatusCode.OWNED_CAR_NOT_FOUND));
        if (!car.getMemberId().equals(memberId)) {
            throw new CustomException(OwnedCarStatusCode.NOT_OWNER);
        }
        return car;
    }

    private OwnedCarSnapshot toSnapshot(OwnedCar car) {
        return new OwnedCarSnapshot(car.getId(), carModelService.getSpecByTrim(car.getCarTrimId()), car.getModelYear());
    }
}

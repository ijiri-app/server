package ijiri.ijiriserver.domain.interestcar.service.impl;

import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;
import ijiri.ijiriserver.domain.interestcar.exception.InterestCarStatusCode;
import ijiri.ijiriserver.domain.interestcar.repository.MemberInterestCarRepository;
import ijiri.ijiriserver.domain.interestcar.service.InterestCarService;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InterestCarServiceImpl implements InterestCarService {

    private final MemberInterestCarRepository memberInterestCarRepository;
    private final MemberService memberService;

    @Override
    @Transactional
    public InterestCarResponse replaceAll(Long memberId, List<Long> carModelIds) {
        // 탈퇴 후 만료 전 access token 으로 들어온 요청이 데이터를 다시 만들지 않도록 활성 회원만 허용
        memberService.getById(memberId);
        if (new HashSet<>(carModelIds).size() != carModelIds.size()) {
            throw new CustomException(InterestCarStatusCode.DUPLICATE_CAR_MODEL);
        }

        memberInterestCarRepository.deleteAllByMemberIdInBulk(memberId);
        List<MemberInterestCar> interestCars = IntStream.range(0, carModelIds.size())
                .mapToObj(order -> MemberInterestCar.builder()
                        .memberId(memberId)
                        .carModelId(carModelIds.get(order))
                        .displayOrder(order)
                        .build()
                )
                .toList();
        return InterestCarResponse.from(memberInterestCarRepository.saveAll(interestCars));
    }

    @EventListener
    @Transactional
    public void removeAll(MemberPurgedEvent event) {
        memberInterestCarRepository.deleteAllByMemberIdInBulk(event.memberId());
    }
}

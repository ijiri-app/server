package ijiri.ijiriserver.domain.interestcar.adapter.out.persistence;

import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort;
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberInterestCarPersistenceAdapter implements MemberInterestCarPort {

    private final MemberInterestCarJpaRepository memberInterestCarJpaRepository;

    @Override
    public List<MemberInterestCar> findAllByMemberIdOrderByDisplayOrderAsc(Long memberId) {
        return memberInterestCarJpaRepository.findAllByMemberIdOrderByDisplayOrderAsc(memberId);
    }

    @Override
    public void deleteAllByMemberIdInBulk(Long memberId) {
        memberInterestCarJpaRepository.deleteAllByMemberIdInBulk(memberId);
    }

    @Override
    public List<MemberInterestCar> saveAll(Iterable<MemberInterestCar> entities) {
        return memberInterestCarJpaRepository.saveAll(entities);
    }
}

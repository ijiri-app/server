package ijiri.ijiriserver.domain.interestcar.application.port.out;

import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;

import java.util.List;

public interface MemberInterestCarPort {

    List<MemberInterestCar> findAllByMemberIdOrderByDisplayOrderAsc(Long memberId);

    void deleteAllByMemberIdInBulk(Long memberId);

    List<MemberInterestCar> saveAll(Iterable<MemberInterestCar> entities);
}

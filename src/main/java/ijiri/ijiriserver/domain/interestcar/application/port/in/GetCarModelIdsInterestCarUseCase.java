package ijiri.ijiriserver.domain.interestcar.application.port.in;

import java.util.List;

public interface GetCarModelIdsInterestCarUseCase {

    /**
     * 피드 탭 순서대로. 없으면 빈 목록.
     */
    List<Long> getCarModelIds(Long memberId);
}

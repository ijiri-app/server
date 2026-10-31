package ijiri.ijiriserver.domain.interestcar.application.port.in;

import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse;

import java.util.List;

public interface InterestCarUseCase {

    InterestCarResponse getAll(Long memberId);

    void replaceAll(Long memberId, List<Long> carModelIds);

    /**
     * 피드 탭 순서대로. 없으면 빈 목록.
     */
    List<Long> getCarModelIds(Long memberId);
}

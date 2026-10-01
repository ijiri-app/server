package ijiri.ijiriserver.domain.interestcar.service;

import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;

import java.util.List;

public interface InterestCarService {

    InterestCarResponse getAll(Long memberId);

    void replaceAll(Long memberId, List<Long> carModelIds);

    /**
     * 피드 탭 순서대로. 없으면 빈 목록.
     */
    List<Long> getCarModelIds(Long memberId);
}

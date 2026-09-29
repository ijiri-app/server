package ijiri.ijiriserver.domain.interestcar.service;

import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;

import java.util.List;

/**
 * 관심 차종 전체 교체. 온보딩과 편집 화면이 같이 사용한다.
 */
public interface InterestCarUpdateService {

    InterestCarResponse replaceAll(Long memberId, List<Long> carModelIds);
}

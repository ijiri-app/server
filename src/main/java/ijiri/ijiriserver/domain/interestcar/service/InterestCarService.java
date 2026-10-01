package ijiri.ijiriserver.domain.interestcar.service;

import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;

import java.util.List;

public interface InterestCarService {

    InterestCarResponse getAll(Long memberId);

    InterestCarResponse replaceAll(Long memberId, List<Long> carModelIds);
}

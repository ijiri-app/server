package ijiri.ijiriserver.domain.interestcar.application.port.in;

import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse;

public interface GetAllInterestCarUseCase {

    InterestCarResponse getAll(Long memberId);
}

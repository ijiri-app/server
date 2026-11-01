package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse;

public interface GetOwnedCarsUseCase {

    OwnedCarResponse getAll(Long memberId);
}

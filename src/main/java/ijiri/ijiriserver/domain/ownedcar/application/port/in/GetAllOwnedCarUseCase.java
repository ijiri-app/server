package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse;

public interface GetAllOwnedCarUseCase {

    OwnedCarResponse getAll(Long memberId);
}

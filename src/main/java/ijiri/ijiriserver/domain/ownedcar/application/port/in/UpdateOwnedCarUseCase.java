package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarUpdateRequest;

public interface UpdateOwnedCarUseCase {

    void update(Long memberId, Long ownedCarId, OwnedCarUpdateRequest request);
}

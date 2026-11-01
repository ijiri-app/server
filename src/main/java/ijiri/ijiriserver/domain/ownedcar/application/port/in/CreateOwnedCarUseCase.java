package ijiri.ijiriserver.domain.ownedcar.application.port.in;

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarCreateRequest;
import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse;

public interface CreateOwnedCarUseCase {

    OwnedCarResponse create(Long memberId, OwnedCarCreateRequest request);
}

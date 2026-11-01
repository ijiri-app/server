package ijiri.ijiriserver.domain.part.application.port.in;

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse;

public interface GetBrandsUseCase {

    PartResponse getBrands(String keyword);
}

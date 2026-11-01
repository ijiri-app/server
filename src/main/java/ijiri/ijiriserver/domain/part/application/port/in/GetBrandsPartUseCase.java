package ijiri.ijiriserver.domain.part.application.port.in;

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse;

public interface GetBrandsPartUseCase {

    PartResponse getBrands(String keyword);
}

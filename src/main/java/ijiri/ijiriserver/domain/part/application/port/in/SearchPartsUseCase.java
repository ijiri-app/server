package ijiri.ijiriserver.domain.part.application.port.in;

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

public interface SearchPartsUseCase {

    PartResponse search(String keyword, PartCategory category, Long carModelId, int size);
}

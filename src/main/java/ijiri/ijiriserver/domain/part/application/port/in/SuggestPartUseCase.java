package ijiri.ijiriserver.domain.part.application.port.in;

import ijiri.ijiriserver.domain.part.application.data.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

public interface SuggestPartUseCase {

    PartResponse suggest(String keyword, PartCategory category, Long carModelId);
}

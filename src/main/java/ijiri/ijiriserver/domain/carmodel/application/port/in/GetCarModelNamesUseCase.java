package ijiri.ijiriserver.domain.carmodel.application.port.in;

import java.util.Collection;
import java.util.Map;

public interface GetCarModelNamesUseCase {

    Map<Long, String> getModelNames(Collection<Long> carModelIds);
}

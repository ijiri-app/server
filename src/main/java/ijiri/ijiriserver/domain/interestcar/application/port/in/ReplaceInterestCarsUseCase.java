package ijiri.ijiriserver.domain.interestcar.application.port.in;

import java.util.List;

public interface ReplaceInterestCarsUseCase {

    void replaceAll(Long memberId, List<Long> carModelIds);
}

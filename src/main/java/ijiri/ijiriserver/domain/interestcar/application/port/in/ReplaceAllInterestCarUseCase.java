package ijiri.ijiriserver.domain.interestcar.application.port.in;

import java.util.List;

public interface ReplaceAllInterestCarUseCase {

    void replaceAll(Long memberId, List<Long> carModelIds);
}

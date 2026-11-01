package ijiri.ijiriserver.domain.part.adapter.out.persistence;

import ijiri.ijiriserver.domain.part.application.port.out.PartCarModelUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartCarModelUsagePersistenceAdapter implements PartCarModelUsagePort {

    private final PartCarModelUsageJpaRepository partCarModelUsageJpaRepository;

    @Override
    public void add(Long partId, Long carModelId, int delta) {
        partCarModelUsageJpaRepository.add(partId, carModelId, delta);
    }
}

package ijiri.ijiriserver.domain.part.application.port.in;

import java.util.Collection;

public interface RecordUsagePartUseCase {

    /**
     * 게시물에 부품이 달리거나(delta = 1) 빠질 때(delta = -1) 전체·차종별 사용 수를 바꾼다.
     */
    void recordUsage(Collection<Long> partIds, Long carModelId, int delta);
}

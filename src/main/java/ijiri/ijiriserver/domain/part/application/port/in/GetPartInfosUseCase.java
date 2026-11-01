package ijiri.ijiriserver.domain.part.application.port.in;

import ijiri.ijiriserver.domain.part.application.data.PartInfo;

import java.util.Collection;
import java.util.Map;

public interface GetPartInfosUseCase {

    /**
     * 없는 id 는 결과에서 빠진다.
     */
    Map<Long, PartInfo> getPartInfos(Collection<Long> partIds);
}

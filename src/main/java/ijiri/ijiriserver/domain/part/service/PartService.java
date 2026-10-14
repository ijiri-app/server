package ijiri.ijiriserver.domain.part.service;

import ijiri.ijiriserver.domain.part.dto.PartCommand;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.dto.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

import java.util.Collection;
import java.util.Map;

public interface PartService {

    PartResponse search(String keyword, PartCategory category, Long carModelId, int size);

    PartResponse suggest(String keyword, PartCategory category, Long carModelId);

    PartResponse getBrands(String keyword);

    /**
     * 기존 부품을 확인하거나, 정규화한 이름이 같은 부품이 없으면 확인 대기 부품으로 새로 만든다 (브랜드도 같은 방식).
     */
    PartInfo resolve(PartCommand command);

    /**
     * 없는 id 는 결과에서 빠진다.
     */
    Map<Long, PartInfo> getPartInfos(Collection<Long> partIds);

    /**
     * 게시물에 부품이 달리거나(delta = 1) 빠질 때(delta = -1) 전체·차종별 사용 수를 바꾼다.
     */
    void recordUsage(Collection<Long> partIds, Long carModelId, int delta);
}

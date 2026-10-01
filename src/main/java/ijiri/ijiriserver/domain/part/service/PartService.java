package ijiri.ijiriserver.domain.part.service;

import ijiri.ijiriserver.domain.part.dto.PartCommand;
import ijiri.ijiriserver.domain.part.dto.PartInfo;
import ijiri.ijiriserver.domain.part.dto.response.PartResponse;
import ijiri.ijiriserver.domain.part.entity.PartCategory;

import java.util.Collection;
import java.util.Map;

public interface PartService {

    PartResponse suggest(String keyword, PartCategory category);

    PartResponse getParts(String keyword, PartCategory category, Long brandId, Long cursor, int size);

    PartResponse getBrands(String keyword);

    /**
     * 기존 부품을 확인하거나, 같은 이름의 부품이 없으면 새로 만든다 (브랜드도 같은 방식).
     */
    PartInfo resolve(PartCommand command);

    /**
     * 없는 id 는 결과에서 빠진다.
     */
    Map<Long, PartInfo> getPartInfos(Collection<Long> partIds);
}

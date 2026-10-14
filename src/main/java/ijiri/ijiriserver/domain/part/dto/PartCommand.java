package ijiri.ijiriserver.domain.part.dto;

import ijiri.ijiriserver.domain.part.entity.PartCategory;

/**
 * 게시물의 부품. partId 가 있으면 기존 부품, 없으면 brandName(선택) + partName + category 로 찾거나
 * 확인 대기(PENDING) 부품으로 새로 만든다.
 */
public record PartCommand(
        Long partId,
        String brandName,
        String partName,
        PartCategory category
) {
}

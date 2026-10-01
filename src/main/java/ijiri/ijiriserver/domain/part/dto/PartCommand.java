package ijiri.ijiriserver.domain.part.dto;

import ijiri.ijiriserver.domain.part.entity.PartCategory;

/**
 * 게시물 태그의 부품. partId 가 있으면 기존 부품, 없으면 name / brandName(선택) / category 로 찾거나 새로 만든다.
 */
public record PartCommand(
        Long partId,
        String name,
        String brandName,
        PartCategory category
) {
}

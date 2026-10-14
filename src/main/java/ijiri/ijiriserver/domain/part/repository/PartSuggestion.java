package ijiri.ijiriserver.domain.part.repository;

/**
 * 부품 후보 추천 결과 (네이티브 쿼리 프로젝션).
 */
public interface PartSuggestion {

    Long getId();

    Double getScore();
}

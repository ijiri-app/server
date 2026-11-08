package ijiri.ijiriserver.car.part.application.data

/**
 * 부품 후보 추천 결과 (네이티브 쿼리 프로젝션).
 */
interface PartSuggestion {

    val id: Long

    val score: Double
}

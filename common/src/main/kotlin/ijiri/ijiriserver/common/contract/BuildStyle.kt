package ijiri.ijiriserver.common.contract

/**
 * 빌드 방향. 보유 차량과 게시물이 함께 쓰고, 피드의 빌드 방향 칩이 이 값으로 거른다.
 */
enum class BuildStyle {
    DAILY,
    CIRCUIT,
    STANCE,
    OEM_LOOK,
}

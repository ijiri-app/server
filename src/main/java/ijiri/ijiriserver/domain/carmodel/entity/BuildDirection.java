package ijiri.ijiriserver.domain.carmodel.entity;

/**
 * 차량을 꾸미는 방향. 보유 차량과 게시물이 함께 쓰고, 피드의 빌드 방향 칩이 이 값으로 거른다.
 */
public enum BuildDirection {
    OEM_PLUS,
    STREET,
    TRACK,
    DRIFT,
    STANCE,
    DRESS_UP
}

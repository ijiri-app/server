package ijiri.ijiriserver.user.ownedcar.entity

enum class OwnedCarStatus {
    // 지금 타는 차
    OWNED,

    // 이전 차량. 게시물이 연결된 차량은 삭제 대신 이 상태가 된다
    PAST,
}

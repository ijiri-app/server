package ijiri.ijiriserver.user.ownedcar.application.port.out

/**
 * 보유 차량별 게시물 수. 게시물은 service-community 에 있으므로 내부 API 로 읽는다.
 */
interface OwnedCarPostCounter {

    /**
     * 게시물이 없는 차량은 결과에서 빠진다.
     */
    fun countPosts(ownedCarIds: Collection<Long>): Map<Long, Long>
}

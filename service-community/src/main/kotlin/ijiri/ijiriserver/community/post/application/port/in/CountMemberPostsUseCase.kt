package ijiri.ijiriserver.community.post.application.port.`in`

interface CountMemberPostsUseCase {

    /**
     * 마이 페이지의 게시물 수 (공개 게시물만).
     */
    fun countVisiblePosts(memberId: Long): Long

    /**
     * 보유 차량별 게시물 수. 게시물이 없는 차량은 결과에서 빠진다.
     */
    fun countPostsByOwnedCar(ownedCarIds: Collection<Long>): Map<Long, Long>
}

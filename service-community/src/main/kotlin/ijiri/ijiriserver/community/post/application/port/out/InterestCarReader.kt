package ijiri.ijiriserver.community.post.application.port.out

/**
 * 관심 차종은 service-user 에 있다. 피드 탭 순서대로, 없으면 빈 목록.
 */
interface InterestCarReader {

    fun getCarModelIds(memberId: Long): List<Long>
}

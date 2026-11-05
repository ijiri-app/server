package ijiri.ijiriserver.community.post.application.port.`in`

interface HideMemberPostsUseCase {

    /**
     * 탈퇴한 회원의 게시물을 AUTHOR_WITHDRAWN 으로 숨긴다. 여러 번 실행해도 같다.
     */
    fun hideAll(memberId: Long)
}

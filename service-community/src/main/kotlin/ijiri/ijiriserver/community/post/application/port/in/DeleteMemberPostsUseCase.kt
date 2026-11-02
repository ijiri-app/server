package ijiri.ijiriserver.community.post.application.port.`in`

interface DeleteMemberPostsUseCase {

    /**
     * 영구 삭제. 게시물마다 부품·태그·담기를 지우고 사진 삭제 이벤트를 발행한다. 여러 번 실행해도 안전하다.
     */
    fun deleteAll(memberId: Long)
}

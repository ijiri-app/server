package ijiri.ijiriserver.community.wishlist.application.port.`in`

interface HideMemberWishesUseCase {

    /**
     * 탈퇴한 회원의 담기를 담기 수에서 뺀다. 행은 영구 삭제 때 지운다.
     */
    fun hideAll(memberId: Long)
}

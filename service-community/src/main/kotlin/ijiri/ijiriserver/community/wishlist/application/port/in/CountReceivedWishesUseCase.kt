package ijiri.ijiriserver.community.wishlist.application.port.`in`

interface CountReceivedWishesUseCase {

    /**
     * 내 게시물 부품이 받은 담기 수 (본인 담기와 탈퇴한 회원의 담기 제외).
     */
    fun countReceived(memberId: Long): Long
}

package ijiri.ijiriserver.community.post.adapter.`in`.web.controller

import ijiri.ijiriserver.common.contract.MemberCounts
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.community.post.application.port.`in`.CountMemberPostsUseCase
import ijiri.ijiriserver.community.wishlist.application.port.`in`.CountReceivedWishesUseCase
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * service-user 가 마이 페이지·프로필의 게시물 수와 받은 담기 수, 보유 차량별 게시물 수를 읽으려고 부른다.
 */
@Hidden
@RestController
@RequestMapping("/internal")
class CommunityInternalController(
    private val countMemberPostsUseCase: CountMemberPostsUseCase,
    private val countReceivedWishesUseCase: CountReceivedWishesUseCase,
) {

    @GetMapping("/members/{memberId}/counts")
    fun getMemberCounts(@PathVariable memberId: Long): BaseResponse<MemberCounts> =
        BaseResponse.ok(
            MemberCounts(
                countMemberPostsUseCase.countVisiblePosts(memberId),
                countReceivedWishesUseCase.countReceived(memberId),
            ),
        )

    @GetMapping("/owned-cars/post-counts")
    fun getOwnedCarPostCounts(@RequestParam(required = false) ids: List<Long>?): BaseResponse<Map<Long, Long>> =
        BaseResponse.ok(countMemberPostsUseCase.countPostsByOwnedCar(ids.orEmpty()))
}

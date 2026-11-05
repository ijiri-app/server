package ijiri.ijiriserver.user.auth.common.application.service

import ijiri.ijiriserver.user.auth.common.application.port.out.SocialUnlinkClient
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.event.MemberPurgedEvent
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock

class SocialUnlinkServiceTest {

    private val kakao: SocialUnlinkClient = mock {
        on { provider() } doReturn Provider.KAKAO
        on { unlink("kakao-1") } doThrow IllegalStateException("admin key")
    }
    private val service = SocialUnlinkService(listOf(kakao))

    @Test
    fun 영구_삭제_전_연결_끊기가_실패하면_삭제를_롤백시켜_다음날_다시_시도한다() {
        val event = MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", false)

        assertThatThrownBy { service.unlinkBeforePurge(event) }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun 마지막_시도에서는_연결_끊기가_실패해도_삭제를_진행한다() {
        val event = MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", true)

        assertThatCode { service.unlinkBeforePurge(event) }.doesNotThrowAnyException()
    }
}

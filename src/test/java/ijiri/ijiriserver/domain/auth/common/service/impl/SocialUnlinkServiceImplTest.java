package ijiri.ijiriserver.domain.auth.common.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialUnlinkClient;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.event.MemberPurgedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialUnlinkServiceImplTest {

    private final SocialUnlinkClient kakao = mock(SocialUnlinkClient.class);
    private final SocialUnlinkServiceImpl service;

    SocialUnlinkServiceImplTest() {
        when(kakao.provider()).thenReturn(Provider.KAKAO);
        doThrow(new IllegalStateException("admin key")).when(kakao).unlink("kakao-1");
        service = new SocialUnlinkServiceImpl(List.of(kakao));
    }

    @Test
    void 영구_삭제_전_연결_끊기가_실패하면_삭제를_롤백시켜_다음날_다시_시도한다() {
        MemberPurgedEvent event = new MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", false);

        assertThatThrownBy(() -> service.unlinkBeforePurge(event)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 마지막_시도에서는_연결_끊기가_실패해도_삭제를_진행한다() {
        MemberPurgedEvent event = new MemberPurgedEvent(1L, Provider.KAKAO, "kakao-1", true);

        assertThatCode(() -> service.unlinkBeforePurge(event)).doesNotThrowAnyException();
    }
}

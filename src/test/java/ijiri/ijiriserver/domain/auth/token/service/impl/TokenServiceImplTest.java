package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef-0123456789";

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final MemberService memberService = mock(MemberService.class);
    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600, 2419200);
    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(
                refreshTokenRepository,
                memberService,
                jwtProvider,
                Clock.fixed(Instant.now(), ZoneOffset.UTC)
        );
    }

    @Test
    void 발급하면_기존_토큰을_모두_지우고_새_토큰_하나만_저장한다() {
        tokenService.issue(Member.builder().id(7L).role(Role.USER).build());

        InOrder order = inOrder(refreshTokenRepository);
        order.verify(refreshTokenRepository).deleteAllByMemberId(7L);
        order.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 저장되지_않은_토큰은_거부하되_다른_기기의_토큰은_건드리지_않는다() {
        String replacedToken = jwtProvider.createRefreshToken("7", "session-1");
        when(refreshTokenRepository.deleteValidByTokenHash(anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> tokenService.refresh(replacedToken)).isInstanceOf(CustomException.class);
        verify(refreshTokenRepository, never()).deleteAllByMemberId(any());
        verify(memberService, never()).getById(any());
    }

    @Test
    void 세션의_refresh_token_이_남아_있어야_access_token_이_유효하다() {
        when(refreshTokenRepository.existsBySessionIdAndExpiresAtAfter(eq("session-1"), any())).thenReturn(true);

        assertThat(tokenService.isActive("session-1")).isTrue();
        assertThat(tokenService.isActive("deleted-session")).isFalse();
    }

    @Test
    void access_token_으로는_갱신할_수_없다() {
        String accessToken = jwtProvider.createAccessToken("7", "USER", "session-1");

        assertThatThrownBy(() -> tokenService.refresh(accessToken)).isInstanceOf(CustomException.class);
        verify(refreshTokenRepository, never()).deleteValidByTokenHash(anyString(), any());
    }
}

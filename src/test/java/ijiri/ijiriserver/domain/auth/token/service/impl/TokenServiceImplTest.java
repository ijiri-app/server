package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void 서명이_유효하지만_저장되지_않은_토큰은_재사용으로_보고_회원의_토큰을_모두_폐기한다() {
        String rotatedToken = jwtProvider.createRefreshToken("7");
        when(refreshTokenRepository.deleteValidByTokenHash(anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> tokenService.refresh(rotatedToken)).isInstanceOf(CustomException.class);
        verify(refreshTokenRepository).deleteAllByMemberId(7L);
        verify(memberService, never()).getById(any());
    }

    @Test
    void access_token_으로는_갱신할_수_없다() {
        String accessToken = jwtProvider.createAccessToken("7", "USER");

        assertThatThrownBy(() -> tokenService.refresh(accessToken)).isInstanceOf(CustomException.class);
        verify(refreshTokenRepository, never()).deleteAllByMemberId(any());
    }

    @Test
    void 회원당_상한을_넘는_오래된_토큰을_지운다() {
        when(refreshTokenRepository.findIdsByMemberIdNewestFirst(7L))
                .thenReturn(LongStream.rangeClosed(1, 12).map(i -> 13 - i).boxed().toList());

        tokenService.issue(Member.builder().id(7L).role(Role.USER).build());

        verify(refreshTokenRepository).deleteAllByIdIn(List.of(2L, 1L));
    }
}

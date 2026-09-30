package ijiri.ijiriserver.domain.auth.token.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.token.entity.RefreshToken;
import ijiri.ijiriserver.domain.auth.token.repository.RefreshTokenRepository;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.event.MemberWithdrawnEvent;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.jwt.JwtProvider;
import ijiri.ijiriserver.global.jwt.SessionValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService, SessionValidator {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final Clock clock;

    // 동시 접속 차단: 한 계정은 refresh token 을 하나만 가진다.
    // 새로 발급할 때 기존 토큰(세션)을 모두 지워, 다른 기기의 access token 도 즉시 거부된다
    @Override
    @Transactional
    public AuthResponse issue(Member member) {
        String subject = String.valueOf(member.getId());
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtProvider.createAccessToken(subject, member.getRole().name(), sessionId);
        String refreshToken = jwtProvider.createRefreshToken(subject, sessionId);

        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(jwtProvider.getRefreshTokenValiditySeconds());
        refreshTokenRepository.deleteAllByMemberId(member.getId());
        refreshTokenRepository.save(RefreshToken.of(member.getId(), sessionId, refreshToken, expiresAt));
        return AuthResponse.tokens(accessToken, refreshToken, jwtProvider.getAccessTokenValiditySeconds());
    }

    // 서명이 유효해도 DB 에 없으면 교체(rotation), 로그아웃, 또는 다른 기기 로그인으로 밀려난 토큰이다.
    // 다른 기기 로그인으로 밀려난 경우와 구분할 수 없으므로, 회원의 토큰을 모두 폐기하지 않고 거부만 한다.
    // (그렇게 하면 밀려난 기기의 갱신 시도가 방금 로그인한 기기까지 로그아웃시킨다)
    // 탈취된 토큰이 쓰이면 원래 기기의 갱신이 실패하고, 다시 로그인하는 순간 탈취한 쪽 토큰이 폐기된다
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        Claims claims = jwtProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));
        int deleted = refreshTokenRepository.deleteValidByTokenHash(
                RefreshToken.hash(refreshToken),
                LocalDateTime.now(clock)
        );
        if (deleted == 0) {
            throw new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN);
        }
        // 탈퇴 등으로 활성 회원이 아니면 앱이 로그인 화면으로 가도록 404 가 아닌 401 로 응답한다
        Member member = memberService.findActiveMember(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN));
        return issue(member);
    }

    // 모든 기기에서 로그아웃. 세션(refresh token)이 지워지므로 access token 도 즉시 거부된다
    @Override
    @Transactional
    public AuthResponse signOut(Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
        return AuthResponse.message(AuthStatusCode.SIGNOUT_SUCCESS.getMessage());
    }

    // access token 이 만료돼도 로그아웃할 수 있게 refresh token 으로 세션을 지운다.
    // 저장된 토큰과 해시가 같아야만 지워지므로 서명·만료 검사는 필요 없고, 없는 토큰이어도 같은 응답을 준다
    @Override
    @Transactional
    public AuthResponse signOutByRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteByTokenHash(RefreshToken.hash(refreshToken));
        return AuthResponse.message(AuthStatusCode.SIGNOUT_SUCCESS.getMessage());
    }

    @EventListener
    @Transactional
    public void revokeAll(MemberWithdrawnEvent event) {
        refreshTokenRepository.deleteAllByMemberId(event.memberId());
    }

    // 인증이 필요한 모든 요청에서 호출된다 (session_id 유니크 인덱스 조회 1회)
    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String sessionId) {
        return refreshTokenRepository.existsBySessionIdAndExpiresAtAfter(sessionId, LocalDateTime.now(clock));
    }
}

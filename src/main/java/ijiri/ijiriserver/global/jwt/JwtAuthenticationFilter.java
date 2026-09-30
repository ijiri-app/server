package ijiri.ijiriserver.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtCookieManager jwtCookieManager;
    private final SessionValidator sessionValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        jwtCookieManager.resolveAccessToken(request)
                .filter(jwtProvider::validateAccessToken)
                .filter(this::hasActiveSession)
                .ifPresent(token -> SecurityContextHolder.getContext()
                        .setAuthentication(jwtProvider.getAuthentication(token)));
        chain.doFilter(request, response);
    }

    // 로그아웃/다른 기기 로그인/탈퇴로 세션이 끝났으면 만료 전 access token 도 인증하지 않는다
    private boolean hasActiveSession(String accessToken) {
        String sessionId = jwtProvider.getSessionId(accessToken);
        return sessionId != null && sessionValidator.isActive(sessionId);
    }
}

package ijiri.ijiriserver.global.jwt;

import io.jsonwebtoken.Claims;
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
                .flatMap(jwtProvider::parseAccessToken)
                .filter(this::hasActiveSession)
                .ifPresent(claims -> SecurityContextHolder.getContext()
                        .setAuthentication(jwtProvider.getAuthentication(claims)));
        chain.doFilter(request, response);
    }

    // 로그아웃/다른 기기 로그인/탈퇴로 세션이 끝났으면 만료 전 access token 도 인증하지 않는다
    private boolean hasActiveSession(Claims claims) {
        String sessionId = jwtProvider.getSessionId(claims);
        return sessionId != null && sessionValidator.isActive(sessionId);
    }
}

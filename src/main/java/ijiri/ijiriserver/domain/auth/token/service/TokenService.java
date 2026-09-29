package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface TokenService {

    AuthResponse issue(Member member);

    AuthResponse refresh(String refreshToken);

    AuthResponse deleteTokens(HttpServletRequest request, HttpServletResponse response);
}

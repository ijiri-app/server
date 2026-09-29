package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse signIn(SignInRequest request);

    AuthResponse signOut(HttpServletRequest request, HttpServletResponse response);
}

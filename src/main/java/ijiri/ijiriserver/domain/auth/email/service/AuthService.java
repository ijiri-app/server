package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse signIn(SignInRequest request);

    AuthResponse signOut(Long memberId);
}

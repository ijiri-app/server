package ijiri.ijiriserver.domain.auth.email.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.SignInResponse;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;

public interface AuthService {

    SignInResponse signup(SignupRequest request);

    SignInResponse signIn(SignInRequest request);
}

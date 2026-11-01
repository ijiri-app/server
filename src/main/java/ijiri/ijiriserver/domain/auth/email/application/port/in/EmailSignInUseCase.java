package ijiri.ijiriserver.domain.auth.email.application.port.in;

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.application.data.request.SignInRequest;

public interface EmailSignInUseCase {

    AuthResponse signIn(SignInRequest request);
}

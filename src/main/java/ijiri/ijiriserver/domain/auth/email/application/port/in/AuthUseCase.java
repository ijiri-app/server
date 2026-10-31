package ijiri.ijiriserver.domain.auth.email.application.port.in;

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.application.data.request.PasswordResetRequest;
import ijiri.ijiriserver.domain.auth.email.application.data.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.application.data.request.SignupRequest;

public interface AuthUseCase {

    AuthResponse signup(SignupRequest request);

    AuthResponse signIn(SignInRequest request);

    void signOut(Long memberId);

    void signOutByRefreshToken(String refreshToken);

    void resetPassword(PasswordResetRequest request);
}

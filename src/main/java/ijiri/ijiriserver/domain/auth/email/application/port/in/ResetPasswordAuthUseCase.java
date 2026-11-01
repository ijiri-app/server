package ijiri.ijiriserver.domain.auth.email.application.port.in;

import ijiri.ijiriserver.domain.auth.email.application.data.request.PasswordResetRequest;

public interface ResetPasswordAuthUseCase {

    void resetPassword(PasswordResetRequest request);
}

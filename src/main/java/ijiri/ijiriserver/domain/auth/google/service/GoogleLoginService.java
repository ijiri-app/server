package ijiri.ijiriserver.domain.auth.google.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;

public interface GoogleLoginService {

    LoginResponse login(String googleIdToken);
}

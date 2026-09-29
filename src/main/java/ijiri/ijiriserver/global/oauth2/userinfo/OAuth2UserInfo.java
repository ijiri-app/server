package ijiri.ijiriserver.global.oauth2.userinfo;

import java.util.Map;

public interface OAuth2UserInfo {

    String getProvider();

    String getProviderUserId();

    String getEmail();

    String getName();

    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "google" -> new GoogleUserInfo(attributes);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }
}

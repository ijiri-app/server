package ijiri.ijiriserver.global.oauth2.userinfo;

import java.util.Map;

public record KakaoUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderUserId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        return (String) account().get("email");
    }

    @Override
    public String getName() {
        Object profile = account().get("profile");
        return profile instanceof Map<?, ?> p ? (String) p.get("nickname") : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> account() {
        Object account = attributes.get("kakao_account");
        return account instanceof Map<?, ?> ? (Map<String, Object>) account : Map.of();
    }
}

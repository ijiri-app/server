package ijiri.ijiriserver.global.oauth2.userinfo;

import java.util.Map;

/**
 * Apple 은 id_token 에 이름을 주지 않는다. 이름은 최초 로그인 시 form_post 의 user 파라미터로만 전달된다.
 */
public record AppleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public String getProvider() {
        return "apple";
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return null;
    }
}

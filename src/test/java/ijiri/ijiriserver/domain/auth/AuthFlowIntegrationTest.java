package ijiri.ijiriserver.domain.auth;

import com.jayway.jsonpath.JsonPath;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig 의 공개 경로, 세션 기반 access token 무효화까지 실제 필터 체인으로 검증한다.
 * 요청 제한(RateLimiter)은 메모리 기반이라 같은 컨텍스트의 테스트끼리 IP 별 횟수가 누적되므로,
 * 요청마다 다른 IP 를 써서 테스트 수나 실행 순서와 무관하게 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "abcd1234";
    private static final AtomicInteger IP_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationMailClient verificationMailClient;

    @Test
    void 가입하면_로그인_응답을_받고_내_정보를_조회할_수_있다() throws Exception {
        String email = newEmail();
        String token = verificationToken(email, 1);

        String json = postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isNewMember").value(true))
                .andExpect(jsonPath("$.result.member.id").exists())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(json))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value(email))
                .andExpect(jsonPath("$.result.postCount").value(0));
    }

    @Test
    void 인증_없이_보호된_API_를_부르면_401() throws Exception {
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 이미_가입된_이메일로_가입_코드를_요청해도_같은_응답을_주고_안내_메일만_보낸다() throws Exception {
        String email = newEmail();
        signup(email);

        postJson("/auth/email/send-code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isNoContent());

        verify(verificationMailClient).sendAlreadyRegistered(email);
        verify(verificationMailClient, times(1)).sendCode(eq(email), anyString(), anyLong());
    }

    @Test
    void 인증_코드가_틀리면_남은_시도_횟수를_준다() throws Exception {
        String email = newEmail();
        postJson("/auth/email/send-code", "{\"email\":\"%s\"}".formatted(email)).andExpect(status().isNoContent());
        String wrong = sentCode(email, 1).equals("000000") ? "111111" : "000000";

        postJson("/auth/email/verify-code", "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, wrong))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
                .andExpect(jsonPath("$.result.remainingAttempts").value(4));
    }

    @Test
    void 같은_닉네임으로는_가입할_수_없다() throws Exception {
        String nickname = uniqueNickname();
        String first = newEmail();
        postJson("/auth/signup", signupBody(first, nickname, verificationToken(first, 1)))
                .andExpect(status().isOk());
        String second = newEmail();

        postJson("/auth/signup", signupBody(second, nickname, verificationToken(second, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS"));
    }

    @Test
    void 필수_약관에_동의하지_않으면_가입할_수_없다() throws Exception {
        String email = newEmail();
        String body = """
                {"email":"%s","password":"%s","nickname":"%s","verificationToken":"%s",
                 "agreements":{"age14":true,"terms":true,"privacy":false}}
                """.formatted(email, PASSWORD, uniqueNickname(), verificationToken(email, 1));

        postJson("/auth/signup", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.result.fields['agreements.privacy']").exists());
    }

    @Test
    void verificationToken_은_한_번만_쓸_수_있다() throws Exception {
        String email = newEmail();
        String token = verificationToken(email, 1);
        postJson("/auth/signup", signupBody(email, uniqueNickname(), token)).andExpect(status().isOk());

        postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"));
    }

    @Test
    void 로그인하면_이전_세션의_access_token_은_즉시_거부된다() throws Exception {
        String email = newEmail();
        Tokens first = signup(email);

        Tokens second = tokens(postJson("/auth/login/email", credentials(email, PASSWORD))
                .andExpect(status().isOk()));

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(first.accessToken())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(second.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void 갱신은_인증_없이_호출되고_쓴_refresh_token_은_다시_쓸_수_없다() throws Exception {
        Tokens tokens = signup(newEmail());
        String body = "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken());

        Tokens refreshed = tokens(postJson("/auth/refresh", body).andExpect(status().isOk()));

        postJson("/auth/refresh", body)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(refreshed.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void 로그아웃하면_access_token_과_refresh_token_이_모두_거부된다() throws Exception {
        Tokens tokens = signup(newEmail());

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void access_token_없이_refresh_token_만으로도_로그아웃할_수_있다() throws Exception {
        Tokens tokens = signup(newEmail());

        postJson("/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰이_하나도_없으면_로그인_상태가_아니므로_로그아웃은_401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 이미_지워진_refresh_token_으로_로그아웃해도_남은_쿠키를_만료시킨다() throws Exception {
        Tokens tokens = signup(newEmail());
        String body = "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken());
        postJson("/auth/logout", body).andExpect(status().isNoContent());

        postJson("/auth/logout", body)
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    void 웹은_body_없이_refreshToken_쿠키만으로_로그아웃한다() throws Exception {
        Tokens tokens = signup(newEmail());

        Cookie refreshCookie = new Cookie("refreshToken", tokens.refreshToken());
        mockMvc.perform(post("/auth/logout").cookie(refreshCookie).with(uniqueIp()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        postJson("/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 탈퇴하면_세션이_끊기고_같은_계정으로_로그인할_수_없다() throws Exception {
        String email = newEmail();
        Tokens tokens = signup(email);

        mockMvc.perform(delete("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/auth/login/email", credentials(email, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWN"));
    }

    @Test
    void 로그인에_5번_실패하면_맞는_비밀번호도_ACCOUNT_LOCKED_로_거부된다() throws Exception {
        String email = newEmail();
        signup(email);
        for (int i = 0; i < 5; i++) {
            postJson("/auth/login/email", credentials(email, "wrong1234"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        postJson("/auth/login/email", credentials(email, PASSWORD))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.result.retryAfterSeconds").isNumber());
    }

    @Test
    void 비밀번호를_재설정하면_기존_세션이_끊기고_새_비밀번호로_로그인한다() throws Exception {
        String email = newEmail();
        Tokens tokens = signup(email);
        postJson("/auth/password/reset/code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isNoContent());

        postJson(
                "/auth/password/reset",
                "{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"newpass123\"}"
                        .formatted(email, sentCode(email, 2))
        ).andExpect(status().isNoContent());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/auth/login/email", credentials(email, "newpass123"))
                .andExpect(status().isOk());
    }

    @Test
    void 가입하지_않은_이메일로_재설정_코드를_요청해도_204_를_주고_메일은_보내지_않는다() throws Exception {
        String email = newEmail();

        postJson("/auth/password/reset/code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isNoContent());

        verify(verificationMailClient, never()).sendCode(eq(email), anyString(), anyLong());
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private Tokens signup(String email) throws Exception {
        String token = verificationToken(email, 1);
        return tokens(postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
                .andExpect(status().isOk()));
    }

    // sentCount: 이 이메일로 지금까지 발송된 코드 수 (마지막 코드를 쓴다)
    private String verificationToken(String email, int sentCount) throws Exception {
        postJson("/auth/email/send-code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isNoContent());
        String json = postJson(
                "/auth/email/verify-code",
                "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, sentCode(email, sentCount))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.result.verificationToken");
    }

    private String sentCode(String email, int sentCount) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationMailClient, times(sentCount)).sendCode(eq(email), code.capture(), anyLong());
        return code.getValue();
    }

    private String signupBody(String email, String nickname, String verificationToken) {
        return """
                {"email":"%s","password":"%s","nickname":"%s","verificationToken":"%s",
                 "agreements":{"age14":true,"terms":true,"privacy":true}}
                """.formatted(email, PASSWORD, nickname, verificationToken);
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).with(uniqueIp()));
    }

    private RequestPostProcessor uniqueIp() {
        return request -> {
            int sequence = IP_SEQUENCE.getAndIncrement();
            request.setRemoteAddr("10.0.%d.%d".formatted(sequence / 250, sequence % 250));
            return request;
        };
    }

    private Tokens tokens(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return new Tokens(accessToken(json), JsonPath.read(json, "$.result.refreshToken"));
    }

    private String accessToken(String json) {
        return JsonPath.read(json, "$.result.accessToken");
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String newEmail() {
        return UUID.randomUUID() + "@ijiri.com";
    }

    // 닉네임은 중복 불가라 테스트마다 다르게 만든다 (2~12자)
    private String uniqueNickname() {
        return "n" + UUID.randomUUID().toString().substring(0, 10);
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}

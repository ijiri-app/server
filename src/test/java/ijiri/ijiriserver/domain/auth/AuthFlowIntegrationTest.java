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
    void 가입하면_토큰이_발급되고_내_정보를_조회할_수_있다() throws Exception {
        Tokens tokens = signup(newEmail());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickname").value("이지리"));
    }

    @Test
    void 인증_없이_보호된_API_를_부르면_401() throws Exception {
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON401"));
    }

    @Test
    void 로그인하면_이전_세션의_access_token_은_즉시_거부된다() throws Exception {
        String email = newEmail();
        Tokens first = signup(email);

        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);
        Tokens second = tokens(postJson("/auth/signin", body).andExpect(status().isOk()));

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(first.accessToken())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(second.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void 갱신은_인증_없이_호출되고_쓴_refresh_token_은_다시_쓸_수_없다() throws Exception {
        Tokens tokens = signup(newEmail());
        String body = "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken());

        Tokens refreshed = tokens(postJson("/token/refresh", body).andExpect(status().isOk()));

        postJson("/token/refresh", body)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(refreshed.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void 로그아웃하면_access_token_과_refresh_token_이_모두_거부된다() throws Exception {
        Tokens tokens = signup(newEmail());

        mockMvc.perform(post("/auth/signout").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/token/refresh", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void access_token_없이_refresh_token_만으로도_로그아웃할_수_있다() throws Exception {
        Tokens tokens = signup(newEmail());

        postJson("/auth/signout", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰이_하나도_없으면_로그인_상태가_아니므로_로그아웃은_401() throws Exception {
        mockMvc.perform(post("/auth/signout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4012"));
    }

    @Test
    void 이미_지워진_refresh_token_으로_로그아웃해도_남은_쿠키를_만료시킨다() throws Exception {
        Tokens tokens = signup(newEmail());
        String body = "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken());
        postJson("/auth/signout", body).andExpect(status().isOk());

        postJson("/auth/signout", body)
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    void 웹은_body_없이_refreshToken_쿠키만으로_로그아웃한다() throws Exception {
        Tokens tokens = signup(newEmail());

        Cookie refreshCookie = new Cookie("refreshToken", tokens.refreshToken());
        mockMvc.perform(post("/auth/signout").cookie(refreshCookie).with(uniqueIp()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        postJson("/token/refresh", "{\"refreshToken\":\"%s\"}".formatted(tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 탈퇴하면_세션이_끊기고_같은_계정으로_로그인할_수_없다() throws Exception {
        String email = newEmail();
        Tokens tokens = signup(email);

        mockMvc.perform(delete("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/auth/signin", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER403"));
    }

    @Test
    void 로그인에_5회_실패하면_맞는_비밀번호도_잠금으로_거부된다() throws Exception {
        String email = newEmail();
        signup(email);
        String wrong = "{\"email\":\"%s\",\"password\":\"wrong1234\"}".formatted(email);
        for (int i = 0; i < 5; i++) {
            postJson("/auth/signin", wrong)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH4013"));
        }

        postJson("/auth/signin", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH4293"));
    }

    @Test
    void 비밀번호를_재설정하면_기존_세션이_끊기고_새_비밀번호로_로그인한다() throws Exception {
        String email = newEmail();
        Tokens tokens = signup(email);

        postJson(
                "/auth/email/verification-code",
                "{\"email\":\"%s\",\"purpose\":\"RESET_PASSWORD\"}".formatted(email)
        ).andExpect(status().isOk());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationMailClient, times(2)).sendCode(eq(email), code.capture(), anyLong());
        postJson(
                "/auth/email/verification-code/verify",
                "{\"email\":\"%s\",\"code\":\"%s\",\"purpose\":\"RESET_PASSWORD\"}"
                        .formatted(email, code.getValue())
        ).andExpect(status().isOk());

        postJson("/auth/password/reset", "{\"email\":\"%s\",\"newPassword\":\"newpass123\"}".formatted(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH2005"));

        mockMvc.perform(get("/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        postJson("/auth/signin", "{\"email\":\"%s\",\"password\":\"newpass123\"}".formatted(email))
                .andExpect(status().isOk());
    }

    @Test
    void 가입하지_않은_이메일로_재설정_코드를_요청해도_같은_응답을_주고_메일은_보내지_않는다() throws Exception {
        String email = newEmail();

        postJson(
                "/auth/email/verification-code",
                "{\"email\":\"%s\",\"purpose\":\"RESET_PASSWORD\"}".formatted(email)
        ).andExpect(status().isOk());

        verify(verificationMailClient, never()).sendCode(eq(email), anyString(), anyLong());
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private Tokens signup(String email) throws Exception {
        postJson("/auth/email/verification-code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isOk());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationMailClient).sendCode(eq(email), code.capture(), anyLong());

        postJson(
                "/auth/email/verification-code/verify",
                "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code.getValue())
        ).andExpect(status().isOk());

        return tokens(postJson(
                "/auth/signup",
                "{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"이지리\"}".formatted(email, PASSWORD)
        ).andExpect(status().isCreated()));
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
        return new Tokens(JsonPath.read(json, "$.result.accessToken"), JsonPath.read(json, "$.result.refreshToken"));
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String newEmail() {
        return UUID.randomUUID() + "@ijiri.com";
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}

package ijiri.ijiriserver.domain.auth;

import com.jayway.jsonpath.JsonPath;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailClient;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig 의 공개 경로, 세션 기반 access token 무효화까지 실제 필터 체인으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "abcd1234";

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
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
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

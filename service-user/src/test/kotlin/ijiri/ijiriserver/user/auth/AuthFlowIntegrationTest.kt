package ijiri.ijiriserver.user.auth

import com.jayway.jsonpath.JsonPath
import ijiri.ijiriserver.user.SynchronousTaskExecutorConfig
import ijiri.ijiriserver.common.contract.MemberCounts
import ijiri.ijiriserver.common.messaging.MessagePublisher
import ijiri.ijiriserver.common.security.GatewayHeaders
import ijiri.ijiriserver.user.auth.email.adapter.out.external.VerificationMailClient
import ijiri.ijiriserver.user.member.application.port.out.MemberCountReader
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * SecurityConfig 의 공개 경로, 게이트웨이 검증(/internal/auth/verify)과 세션 기반 access token 무효화까지 실제 필터 체인으로
 * 검증한다. nginx 가 하는 일(검증 결과 헤더를 요청에 붙이기)은 viaGateway 가 흉내 낸다. 다른 서비스 호출과 Kafka 는 목이다.
 * 요청 제한(RateLimiter)은 메모리 기반이라 같은 컨텍스트의 테스트끼리 IP 별 횟수가 누적되므로,
 * 요청마다 다른 IP 를 써서 테스트 수나 실행 순서와 무관하게 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
@Import(SynchronousTaskExecutorConfig::class)
class AuthFlowIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    lateinit var verificationMailClient: VerificationMailClient

    @MockitoBean
    lateinit var memberCountReader: MemberCountReader

    @MockitoBean
    lateinit var messagePublisher: MessagePublisher

    @BeforeEach
    fun stubOtherServices() {
        whenever(memberCountReader.countsOf(any())).thenReturn(MemberCounts(0, 0))
    }

    @Test
    fun 가입하면_로그인_응답을_받고_내_정보를_조회할_수_있다() {
        val email = newEmail()
        val token = verificationToken(email, 1)

        val json = postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.isNewMember").value(true))
            .andExpect(jsonPath("$.result.member.id").exists())
            .andReturn().response.contentAsString

        mockMvc.perform(viaGateway(get("/members/me"), accessToken(json)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.email").value(email))
            .andExpect(jsonPath("$.result.postCount").value(0))
    }

    @Test
    fun 게이트웨이_검증은_쿠키_토큰을_허용된_Origin_에서만_받고_역할은_DB_값을_준다() {
        val email = newEmail()
        val tokens = signup(email)
        val cookie = Cookie("accessToken", tokens.accessToken)

        mockMvc.perform(get("/internal/auth/verify").cookie(cookie).header(HttpHeaders.ORIGIN, "http://localhost:3000"))
            .andExpect(status().isOk())
            .andExpect(header().string(GatewayHeaders.MEMBER_ROLE, "USER"))
        // 허용되지 않은 Origin 은 쿠키를 보기 전에 CORS 필터가 거절한다. 같은 응답이 nginx 를 거쳐 클라이언트에 간다
        mockMvc.perform(get("/internal/auth/verify").cookie(cookie).header(HttpHeaders.ORIGIN, "https://evil.example"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(GatewayHeaders.MEMBER_ID))

        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE email = ?", email)
        mockMvc.perform(get("/internal/auth/verify").header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken)))
            .andExpect(header().string(GatewayHeaders.MEMBER_ROLE, "ADMIN"))
    }

    @Test
    fun 인증_없이_보호된_API_를_부르면_401() {
        mockMvc.perform(get("/members/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun 이미_가입된_이메일로_가입_코드를_요청해도_같은_응답을_주고_안내_메일만_보낸다() {
        val email = newEmail()
        signup(email)

        postJson("/auth/email/send-code", "{\"email\":\"$email\"}")
            .andExpect(status().isNoContent())

        verify(verificationMailClient).sendAlreadyRegistered(email)
        verify(verificationMailClient, times(1)).sendCode(eq(email), any(), any())
    }

    @Test
    fun 가입된_이메일도_틀린_가입_코드에_같은_남은_횟수를_주어_가입_여부가_드러나지_않는다() {
        val email = newEmail()
        signup(email)
        postJson("/auth/email/send-code", "{\"email\":\"$email\"}").andExpect(status().isNoContent())

        postJson("/auth/email/verify-code", "{\"email\":\"$email\",\"code\":\"${wrongCode(email, "SIGNUP")}\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(jsonPath("$.result.remainingAttempts").value(4))
        verify(verificationMailClient, times(1)).sendCode(eq(email), any(), any())
    }

    @Test
    fun 가입하지_않은_이메일도_틀린_재설정_코드에_같은_남은_횟수를_준다() {
        val email = newEmail()
        postJson("/auth/password/reset/code", "{\"email\":\"$email\"}").andExpect(status().isNoContent())

        postJson(
            "/auth/password/reset",
            "{\"email\":\"$email\",\"code\":\"${wrongCode(email, "RESET_PASSWORD")}\",\"newPassword\":\"newpass123\"}",
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(jsonPath("$.result.remainingAttempts").value(4))
        verify(verificationMailClient, never()).sendCode(eq(email), any(), any())
    }

    @Test
    fun 인증_코드가_틀리면_남은_시도_횟수를_준다() {
        val email = newEmail()
        postJson("/auth/email/send-code", "{\"email\":\"$email\"}").andExpect(status().isNoContent())
        val wrong = if (sentCode(email, 1) == "000000") "111111" else "000000"

        postJson("/auth/email/verify-code", "{\"email\":\"$email\",\"code\":\"$wrong\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(jsonPath("$.result.remainingAttempts").value(4))
    }

    @Test
    fun 같은_닉네임으로는_가입할_수_없다() {
        val nickname = uniqueNickname()
        val first = newEmail()
        postJson("/auth/signup", signupBody(first, nickname, verificationToken(first, 1)))
            .andExpect(status().isOk())
        val second = newEmail()

        postJson("/auth/signup", signupBody(second, nickname, verificationToken(second, 1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS"))
    }

    @Test
    fun 필수_약관에_동의하지_않으면_가입할_수_없다() {
        val email = newEmail()
        val body = """
            {"email":"$email","password":"$PASSWORD","nickname":"${uniqueNickname()}",
             "verificationToken":"${verificationToken(email, 1)}",
             "agreements":{"age14":true,"terms":true,"privacy":false}}
        """.trimIndent()

        postJson("/auth/signup", body)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.result.fields['agreements.privacy']").exists())
    }

    @Test
    fun verificationToken_은_한_번만_쓸_수_있다() {
        val email = newEmail()
        val token = verificationToken(email, 1)
        postJson("/auth/signup", signupBody(email, uniqueNickname(), token)).andExpect(status().isOk())

        postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"))
    }

    @Test
    fun 로그인하면_이전_세션의_access_token_은_즉시_거부된다() {
        val email = newEmail()
        val first = signup(email)

        val second = tokens(
            postJson("/auth/login/email", credentials(email, PASSWORD))
                .andExpect(status().isOk()),
        )

        mockMvc.perform(viaGateway(get("/members/me"), first.accessToken))
            .andExpect(status().isUnauthorized())
        mockMvc.perform(viaGateway(get("/members/me"), second.accessToken))
            .andExpect(status().isOk())
    }

    @Test
    fun 갱신은_인증_없이_호출되고_쓴_refresh_token_은_다시_쓸_수_없다() {
        val tokens = signup(newEmail())
        val body = "{\"refreshToken\":\"${tokens.refreshToken}\"}"

        val refreshed = tokens(postJson("/auth/refresh", body).andExpect(status().isOk()))

        postJson("/auth/refresh", body)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
        mockMvc.perform(viaGateway(get("/members/me"), refreshed.accessToken))
            .andExpect(status().isOk())
    }

    @Test
    fun 로그아웃하면_access_token_과_refresh_token_이_모두_거부된다() {
        val tokens = signup(newEmail())

        mockMvc.perform(viaGateway(post("/auth/logout"), tokens.accessToken))
            .andExpect(status().isNoContent())

        mockMvc.perform(viaGateway(get("/members/me"), tokens.accessToken))
            .andExpect(status().isUnauthorized())
        postJson("/auth/refresh", "{\"refreshToken\":\"${tokens.refreshToken}\"}")
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun access_token_없이_refresh_token_만으로도_로그아웃할_수_있다() {
        val tokens = signup(newEmail())

        postJson("/auth/logout", "{\"refreshToken\":\"${tokens.refreshToken}\"}")
            .andExpect(status().isNoContent())

        mockMvc.perform(viaGateway(get("/members/me"), tokens.accessToken))
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun 토큰이_하나도_없으면_로그인_상태가_아니므로_로그아웃은_401() {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
    }

    @Test
    fun 이미_지워진_refresh_token_으로_로그아웃해도_남은_쿠키를_만료시킨다() {
        val tokens = signup(newEmail())
        val body = "{\"refreshToken\":\"${tokens.refreshToken}\"}"
        postJson("/auth/logout", body).andExpect(status().isNoContent())

        postJson("/auth/logout", body)
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("accessToken", 0))
            .andExpect(cookie().maxAge("refreshToken", 0))
    }

    @Test
    fun 웹은_body_없이_refreshToken_쿠키만으로_로그아웃한다() {
        val tokens = signup(newEmail())

        val refreshCookie = Cookie("refreshToken", tokens.refreshToken)
        mockMvc.perform(post("/auth/logout").cookie(refreshCookie).with(uniqueIp()))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("accessToken", 0))
            .andExpect(cookie().maxAge("refreshToken", 0))

        postJson("/auth/refresh", "{\"refreshToken\":\"${tokens.refreshToken}\"}")
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun 탈퇴하면_세션이_끊기고_같은_계정으로_로그인할_수_없다() {
        val email = newEmail()
        val tokens = signup(email)

        mockMvc.perform(viaGateway(delete("/members/me"), tokens.accessToken))
            .andExpect(status().isNoContent())

        mockMvc.perform(viaGateway(get("/members/me"), tokens.accessToken))
            .andExpect(status().isUnauthorized())
        postJson("/auth/login/email", credentials(email, PASSWORD))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWN"))
    }

    @Test
    fun 로그인에_5번_실패하면_맞는_비밀번호도_ACCOUNT_LOCKED_로_거부된다() {
        val email = newEmail()
        signup(email)
        repeat(5) {
            postJson("/auth/login/email", credentials(email, "wrong1234"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        }

        postJson("/auth/login/email", credentials(email, PASSWORD))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
            .andExpect(jsonPath("$.result.retryAfterSeconds").isNumber())
    }

    @Test
    fun 비밀번호를_재설정하면_기존_세션이_끊기고_새_비밀번호로_로그인한다() {
        val email = newEmail()
        val tokens = signup(email)
        postJson("/auth/password/reset/code", "{\"email\":\"$email\"}")
            .andExpect(status().isNoContent())

        postJson(
            "/auth/password/reset",
            "{\"email\":\"$email\",\"code\":\"${sentCode(email, 2)}\",\"newPassword\":\"newpass123\"}",
        ).andExpect(status().isNoContent())

        mockMvc.perform(viaGateway(get("/members/me"), tokens.accessToken))
            .andExpect(status().isUnauthorized())
        postJson("/auth/login/email", credentials(email, "newpass123"))
            .andExpect(status().isOk())
    }

    @Test
    fun 가입하지_않은_이메일로_재설정_코드를_요청해도_204_를_주고_메일은_보내지_않는다() {
        val email = newEmail()

        postJson("/auth/password/reset/code", "{\"email\":\"$email\"}")
            .andExpect(status().isNoContent())

        verify(verificationMailClient, never()).sendCode(eq(email), any(), any())
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private fun signup(email: String): Tokens {
        val token = verificationToken(email, 1)
        return tokens(
            postJson("/auth/signup", signupBody(email, uniqueNickname(), token))
                .andExpect(status().isOk()),
        )
    }

    // sentCount: 이 이메일로 지금까지 발송된 코드 수 (마지막 코드를 쓴다)
    private fun verificationToken(email: String, sentCount: Int): String {
        postJson("/auth/email/send-code", "{\"email\":\"$email\"}")
            .andExpect(status().isNoContent())
        val json = postJson(
            "/auth/email/verify-code",
            "{\"email\":\"$email\",\"code\":\"${sentCode(email, sentCount)}\"}",
        ).andExpect(status().isOk()).andReturn().response.contentAsString
        return JsonPath.read(json, "$.result.verificationToken")
    }

    // 보내지 않은 코드는 DB 에서 읽어 그와 다른 값을 만든다
    private fun wrongCode(email: String, purpose: String): String {
        val code = jdbcTemplate.queryForObject(
            "SELECT code FROM email_verification WHERE email = ? AND purpose = ?",
            String::class.java,
            email,
            purpose,
        )
        return if (code == "000000") "111111" else "000000"
    }

    private fun sentCode(email: String, sentCount: Int): String {
        val code = argumentCaptor<String>()
        verify(verificationMailClient, times(sentCount)).sendCode(eq(email), code.capture(), any())
        return code.lastValue
    }

    private fun signupBody(email: String, nickname: String, verificationToken: String): String =
        """
            {"email":"$email","password":"$PASSWORD","nickname":"$nickname","verificationToken":"$verificationToken",
             "agreements":{"age14":true,"terms":true,"privacy":true}}
        """.trimIndent()

    private fun credentials(email: String, password: String): String =
        "{\"email\":\"$email\",\"password\":\"$password\"}"

    private fun postJson(url: String, body: String): ResultActions =
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).with(uniqueIp()))

    private fun uniqueIp(): RequestPostProcessor =
        RequestPostProcessor { request ->
            val sequence = IP_SEQUENCE.getAndIncrement()
            request.remoteAddr = "10.0.${sequence / 250}.${sequence % 250}"
            request
        }

    private fun tokens(result: ResultActions): Tokens {
        val json = result.andReturn().response.contentAsString
        return Tokens(accessToken(json), JsonPath.read(json, "$.result.refreshToken"))
    }

    private fun accessToken(json: String): String = JsonPath.read(json, "$.result.accessToken")

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    // nginx 의 auth_request 를 흉내 낸다. 검증 결과 헤더(X-Member-Id, X-Member-Role)를 실제 요청에 붙인다
    private fun viaGateway(builder: MockHttpServletRequestBuilder, accessToken: String): MockHttpServletRequestBuilder {
        val verified = mockMvc.perform(
            get("/internal/auth/verify").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)),
        )
            .andExpect(status().isOk())
            .andReturn().response
        verified.getHeader(GatewayHeaders.MEMBER_ID)?.let { memberId ->
            builder.header(GatewayHeaders.MEMBER_ID, memberId)
            builder.header(GatewayHeaders.MEMBER_ROLE, verified.getHeader(GatewayHeaders.MEMBER_ROLE)!!)
        }
        return builder.with(uniqueIp())
    }

    private fun newEmail(): String = "${UUID.randomUUID()}@ijiri.com"

    // 닉네임은 중복 불가라 테스트마다 다르게 만든다 (2~12자)
    private fun uniqueNickname(): String = "n" + UUID.randomUUID().toString().substring(0, 10)

    private data class Tokens(val accessToken: String, val refreshToken: String)

    companion object {
        private const val PASSWORD = "abcd1234"
        private val IP_SEQUENCE = AtomicInteger()
    }
}

package ijiri.ijiriserver.domain;

import com.jayway.jsonpath.JsonPath;
import ijiri.ijiriserver.domain.auth.email.client.VerificationMailClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 차종 마스터 -> 보유 차량 -> 사진 업로드 -> 게시물(부품 태그) -> 피드/상세 -> 위시리스트 -> 차단 -> 신고 -> 관리자 처리를
 * 실제 필터 체인으로 검증한다. 요청 제한이 테스트끼리 누적되지 않도록 요청마다 다른 IP 를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFlowIntegrationTest {

    private static final String PASSWORD = "abcd1234";
    private static final AtomicInteger IP_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private VerificationMailClient verificationMailClient;

    @Test
    void 차종_마스터는_로그인_없이_조회한다() throws Exception {
        mockMvc.perform(get("/car-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.carModels[0].name").value("아반떼 N"));

        long modelId = firstCarModelId();
        mockMvc.perform(get("/car-models/" + modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.carModel.generations[0].trims[0].name").exists());
    }

    @Test
    void 게시물을_올리면_피드와_상세에_부품이_보이고_위시리스트에_담을_수_있다() throws Exception {
        String token = signup(newEmail());
        long postId = createPost(token, "N 퍼포먼스 머플러");

        mockMvc.perform(get("/feed").param("carModelIds", String.valueOf(firstCarModelId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.posts[*].id", hasItem((int) postId)));

        String detail = mockMvc.perform(get("/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.partCount").value(1))
                .andExpect(jsonPath("$.result.images[0].width").value(3))
                .andExpect(jsonPath("$.result.partGroups[0].category").value("POWERTRAIN"))
                .andExpect(jsonPath("$.result.partGroups[0].parts[0].brandName").value("현대 N"))
                .andReturn().getResponse().getContentAsString();
        int partId = JsonPath.read(detail, "$.result.partGroups[0].parts[0].partId");

        String other = signup(newEmail());
        mockMvc.perform(authorized(post("/wishlist"), other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partId\":%d}".formatted(partId)))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(get("/posts/" + postId), other))
                .andExpect(jsonPath("$.result.partGroups[0].parts[0].wishlisted").value(true));
        mockMvc.perform(authorized(get("/members/me/wishlist"), other))
                .andExpect(jsonPath("$.result.items[0].partId").value(partId));
    }

    @Test
    void 같은_이름의_새_부품은_대소문자_공백이_달라도_하나로_등록된다() throws Exception {
        String token = signup(newEmail());
        String partName = "Coilover " + UUID.randomUUID();
        long first = createPost(token, partName);
        long second = createPost(token, " " + partName.toUpperCase() + " ");

        mockMvc.perform(get("/posts/" + first))
                .andExpect(jsonPath("$.result.partGroups[0].parts[0].partId").value((int) partIdOf(second)));
    }

    @Test
    void 차단하면_서로의_게시물이_보이지_않는다() throws Exception {
        String author = signup(newEmail());
        long postId = createPost(author, "브레이크 패드");
        long authorId = myId(author);
        String viewer = signup(newEmail());

        mockMvc.perform(authorized(post("/members/" + authorId + "/block"), viewer))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/feed"), viewer))
                .andExpect(jsonPath("$.result.posts[*].id", not(hasItem((int) postId))));
        mockMvc.perform(authorized(get("/posts/" + postId), viewer))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(get("/members/me/blocks"), viewer))
                .andExpect(jsonPath("$.result.blockedMembers[0].memberId").value((int) authorId));
    }

    @Test
    void 관리자가_신고된_게시물을_숨기면_피드에서_빠진다() throws Exception {
        String author = signup(newEmail());
        long postId = createPost(author, "사이드 스커트");
        String reporter = signup(newEmail());

        mockMvc.perform(authorized(post("/reports"), reporter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"POST\",\"targetId\":%d,\"reason\":\"SPAM\"}".formatted(postId)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/admin/reports"), reporter))
                .andExpect(status().isForbidden());

        String adminEmail = newEmail();
        signup(adminEmail);
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE provider_member_id = ?", adminEmail);
        String admin = signIn(adminEmail);

        String reports = mockMvc.perform(authorized(get("/admin/reports"), admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> reportIds = JsonPath.read(reports, "$.result.reports[?(@.targetId == %d)].id".formatted(postId));

        mockMvc.perform(authorized(patch("/admin/reports/" + reportIds.getFirst()), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HIDE_POST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.report.status").value("ACTIONED"));

        mockMvc.perform(get("/posts/" + postId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/feed"))
                .andExpect(jsonPath("$.result.posts[*].id", not(hasItem((int) postId))));
    }

    @Test
    void 탈퇴하면_게시물이_비공개되고_이메일이_지워진다() throws Exception {
        String email = newEmail();
        String token = signup(email);
        long postId = createPost(token, "스포일러");

        mockMvc.perform(authorized(delete("/members/me"), token)).andExpect(status().isOk());

        mockMvc.perform(get("/posts/" + postId)).andExpect(status().isNotFound());
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE provider_member_id = ? AND email IS NULL",
                Integer.class,
                email
        );
        assertThat(remaining).isEqualTo(1);
    }

    @Test
    void 다른_사람의_업로드_사진으로는_게시물을_올릴_수_없다() throws Exception {
        String owner = signup(newEmail());
        long imageId = upload(owner);
        String other = signup(newEmail());
        long carId = registerCar(other);

        mockMvc.perform(authorized(post("/posts"), other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownedCarId":%d,"buildDirection":"STREET","images":[{"imageId":%d}]}
                                """.formatted(carId, imageId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD4002"));
    }

    @Test
    void 사진이_아닌_파일은_올릴_수_없다() throws Exception {
        String token = signup(newEmail());
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes());

        mockMvc.perform(authorized(multipart("/uploads/images").file(file), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD4001"));
    }

    private long createPost(String token, String partName) throws Exception {
        long carId = registerCar(token);
        long imageId = upload(token);
        String body = """
                {"ownedCarId":%d,"buildDirection":"STREET","content":"첫 빌드","images":[{"imageId":%d,"tags":[
                {"x":0.5,"y":0.5,"partName":"%s","brandName":"현대 N","category":"POWERTRAIN"}]}]}
                """.formatted(carId, imageId, partName);
        String json = mockMvc.perform(authorized(post("/posts"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    private long partIdOf(long postId) throws Exception {
        String json = mockMvc.perform(get("/posts/" + postId)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.partGroups[0].parts[0].partId")).longValue();
    }

    private long registerCar(String token) throws Exception {
        long modelId = firstCarModelId();
        String detail = mockMvc.perform(get("/car-models/" + modelId)).andReturn().getResponse().getContentAsString();
        int generationId = JsonPath.read(detail, "$.result.carModel.generations[0].id");
        String json = mockMvc.perform(authorized(post("/members/me/cars"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carModelId\":%d,\"carGenerationId\":%d,\"modelYear\":2023}"
                                .formatted(modelId, generationId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.car.id")).longValue();
    }

    private long upload(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", pngBytes());
        String json = mockMvc.perform(authorized(multipart("/uploads/images").file(file), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.imageId")).longValue();
    }

    private long firstCarModelId() throws Exception {
        String json = mockMvc.perform(get("/car-models")).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.carModels[0].id")).longValue();
    }

    private long myId(String token) throws Exception {
        String json = mockMvc.perform(authorized(get("/members/me"), token))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    private byte[] pngBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private String signup(String email) throws Exception {
        postJson("/auth/email/verification-code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isOk());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationMailClient).sendCode(eq(email), code.capture(), anyLong());
        postJson(
                "/auth/email/verification-code/verify",
                "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code.getValue())
        ).andExpect(status().isOk());
        return accessToken(postJson(
                "/auth/signup",
                "{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"이지리\"}".formatted(email, PASSWORD)
        ).andExpect(status().isCreated()));
    }

    private String signIn(String email) throws Exception {
        return accessToken(postJson(
                "/auth/signin",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)
        ).andExpect(status().isOk()));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).with(uniqueIp()));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).with(uniqueIp());
    }

    private RequestPostProcessor uniqueIp() {
        return request -> {
            int sequence = IP_SEQUENCE.getAndIncrement();
            request.setRemoteAddr("10.1.%d.%d".formatted(sequence / 250, sequence % 250));
            return request;
        };
    }

    private String accessToken(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.result.accessToken");
    }

    private String newEmail() {
        return UUID.randomUUID() + "@ijiri.com";
    }
}

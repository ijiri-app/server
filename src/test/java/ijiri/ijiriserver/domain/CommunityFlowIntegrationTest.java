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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 차종 마스터 -> 보유 차량 -> 사진 업로드 -> 게시물(부품·태그) -> 피드/상세 -> 위시리스트 -> 차단 -> 신고 -> 관리자 처리를
 * 실제 필터 체인으로 검증한다. 요청 제한이 테스트끼리 누적되지 않도록 요청마다 다른 IP 를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFlowIntegrationTest {

    private static final String PASSWORD = "abcd1234";
    private static final String HOST = "http://localhost";
    private static final AtomicInteger IP_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private VerificationMailClient verificationMailClient;

    @Test
    void 차종_마스터는_로그인_없이_조회하고_핵심_계열만_거를_수_있다() throws Exception {
        mockMvc.perform(get("/car-models").param("core", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[*].isCore", not(hasItem(false))))
                .andExpect(jsonPath("$.result.items[*].name", hasItem("아반떼 N")));
        mockMvc.perform(get("/car-models").param("q", "n 라인"))
                .andExpect(jsonPath("$.result.items[0].name").value("아반떼 N 라인"));

        mockMvc.perform(get("/car-models/" + avanteNId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.brand").value("현대"))
                .andExpect(jsonPath("$.result.generations[0].trims[0].name").value("2.0T"));
    }

    @Test
    void 게시물을_올리면_피드와_상세에_보이고_다른_사람이_담으면_담기_수가_오른다() throws Exception {
        String author = signup();
        long postId = createPost(author, "TE37 SAGA 18");

        mockMvc.perform(get("/feed").param("carModelId", String.valueOf(avanteNId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[*].postId", hasItem((int) postId)))
                .andExpect(jsonPath("$.result.items[0].thumbWidth").value(1536));

        String detail = mockMvc.perform(get("/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.car.carModelName").value("아반떼 N"))
                .andExpect(jsonPath("$.result.car.year").value(2023))
                // 분류 순서(익스테리어, 인테리어, 파워트레인, 하체)대로
                .andExpect(jsonPath("$.result.parts[0].category").value("EXTERIOR"))
                .andExpect(jsonPath("$.result.parts[1].category").value("CHASSIS"))
                .andExpect(jsonPath("$.result.parts[1].brandName").value("RAYS"))
                .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.42))
                .andExpect(jsonPath("$.result.isMine").value(false))
                .andExpect(jsonPath("$.result.createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")))
                .andReturn().getResponse().getContentAsString();
        int postPartId = JsonPath.read(detail, "$.result.parts[1].postPartId");

        // 작성자 본인의 담기는 담기 수에 세지 않는다
        wish(author, postPartId).andExpect(status().isCreated());
        String other = signup();
        String added = wish(other, postPartId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        wish(other, postPartId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value((int) JsonPath.read(added, "$.result.id")));

        mockMvc.perform(authorized(get("/posts/" + postId), other))
                .andExpect(jsonPath("$.result.parts[1].wishCount").value(1))
                .andExpect(jsonPath("$.result.parts[1].wished").value(true))
                .andExpect(jsonPath("$.result.wishCount").value(1));
        mockMvc.perform(authorized(get("/members/me/wishlist"), other))
                .andExpect(jsonPath("$.result.items[0].postId").value((int) postId))
                .andExpect(jsonPath("$.result.items[0].partName").value("TE37 SAGA 18"))
                .andExpect(jsonPath("$.result.items[0].carModelName").value("아반떼 N"));
        mockMvc.perform(authorized(get("/members/me"), author))
                .andExpect(jsonPath("$.result.postCount").value(1))
                .andExpect(jsonPath("$.result.receivedWishCount").value(1));
    }

    @Test
    void 대소문자_공백_하이픈만_다른_새_부품은_같은_부품이고_검색도_된다() throws Exception {
        String token = signup();
        String name = "TE-37 " + UUID.randomUUID().toString().substring(0, 8);
        long first = createPost(token, name);
        long second = createPost(token, " " + name.toLowerCase().replace("-", "") + " ");

        assertThat(partIdOf(first)).isEqualTo(partIdOf(second));
        mockMvc.perform(authorized(get("/parts"), token).param("q", name.replace("-", "").toUpperCase()))
                .andExpect(jsonPath("$.result.items[0].id").value((int) partIdOf(first)))
                .andExpect(jsonPath("$.result.items[0].useCount").value(2));
        mockMvc.perform(authorized(get("/parts/suggest"), token).param("q", "rays " + name))
                .andExpect(jsonPath("$.result.items[0].id").value((int) partIdOf(first)))
                .andExpect(jsonPath("$.result.items[0].score").value(1.0));
    }

    @Test
    void 부품을_수정해도_postPartId_를_보내면_담기가_남고_빠진_부품의_담기는_지워진다() throws Exception {
        String author = signup();
        long postId = createPost(author, "브레이크 " + UUID.randomUUID().toString().substring(0, 6));
        String detail = mockMvc.perform(get("/posts/" + postId)).andReturn().getResponse().getContentAsString();
        int kept = JsonPath.read(detail, "$.result.parts[0].postPartId");
        int removed = JsonPath.read(detail, "$.result.parts[1].postPartId");
        String imageKey = JsonPath.read(detail, "$.result.images[0].imageKey");
        String other = signup();
        wish(other, kept).andExpect(status().isCreated());
        wish(other, removed).andExpect(status().isCreated());

        mockMvc.perform(authorized(patch("/posts/" + postId), author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buildStyle":"CIRCUIT","parts":[{"ref":"a","category":"CHASSIS","postPartId":%d}],
                                 "images":[{"imageKey":"%s","tags":[{"ref":"a","x":0.1,"y":0.2}]}]}
                                """.formatted(kept, imageKey)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/posts/" + postId), other))
                .andExpect(jsonPath("$.result.car.buildStyle").value("CIRCUIT"))
                .andExpect(jsonPath("$.result.parts.length()").value(1))
                .andExpect(jsonPath("$.result.parts[0].postPartId").value(kept))
                .andExpect(jsonPath("$.result.parts[0].wished").value(true))
                .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.1));
        mockMvc.perform(authorized(get("/members/me/wishlist"), other))
                .andExpect(jsonPath("$.result.items.length()").value(1));
    }

    @Test
    void 부품_없이_태그만_보내면_수정을_거절한다() throws Exception {
        String author = signup();
        long postId = createPost(author, "머플러");

        mockMvc.perform(authorized(patch("/posts/" + postId), author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"imageKey\":\"posts/tmp/x.png\",\"tags\":[]}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 로그인_상태에서_차종을_고르지_않으면_관심_차종_피드를_본다() throws Exception {
        String author = signup();
        long postId = createPost(author, "프론트립");
        String viewer = signup();
        long otherModel = carModelIdByName("M3");
        mockMvc.perform(authorized(put("/members/me/interest-cars"), viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carModelIds\":[%d]}".formatted(otherModel)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/feed"), viewer))
                .andExpect(jsonPath("$.result.items[*].postId", not(hasItem((int) postId))));
        mockMvc.perform(authorized(get("/members/me/interest-cars"), viewer))
                .andExpect(jsonPath("$.result.items[0].name").value("M3"));
    }

    @Test
    void 차단하면_서로의_게시물이_보이지_않는다() throws Exception {
        String author = signup();
        long postId = createPost(author, "사이드 스커트");
        long authorId = myId(author);
        String viewer = signup();

        mockMvc.perform(authorized(post("/members/" + authorId + "/block"), viewer))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/feed").param("carModelId", String.valueOf(avanteNId())), viewer))
                .andExpect(jsonPath("$.result.items[*].postId", not(hasItem((int) postId))));
        mockMvc.perform(authorized(get("/posts/" + postId), viewer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(authorized(get("/members/me/blocks"), viewer))
                .andExpect(jsonPath("$.result.items[0].memberId").value((int) authorId));
    }

    @Test
    void 관리자가_신고된_게시물을_숨기면_피드에서_빠진다_역할은_다시_로그인하지_않아도_반영된다() throws Exception {
        String author = signup();
        long postId = createPost(author, "스포일러");
        String reporter = signup();
        String report = "{\"targetType\":\"POST\",\"targetId\":%d,\"reason\":\"ILLEGAL_TUNING\"}".formatted(postId);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(authorized(post("/reports"), reporter)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(report))
                    .andExpect(status().isCreated());
        }

        String admin = signup();
        mockMvc.perform(authorized(get("/admin/reports"), admin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", myId(admin));

        String reports = mockMvc.perform(authorized(get("/admin/reports"), admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> reportIds = JsonPath.read(reports, "$.result.items[?(@.targetId == %d)].id".formatted(postId));
        List<Integer> counts = JsonPath.read(
                reports,
                "$.result.items[?(@.targetId == %d)].reportCount".formatted(postId)
        );
        assertThat(counts).containsExactly(1);

        mockMvc.perform(authorized(patch("/admin/reports/" + reportIds.getFirst()), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HIDE_POST\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/posts/" + postId)).andExpect(status().isNotFound());
        mockMvc.perform(authorized(get("/members/" + myId(author) + "/posts"), author))
                .andExpect(jsonPath("$.result.items[0].hidden").value(true));
    }

    @Test
    void 탈퇴하면_게시물이_비공개되고_이메일이_지워진다() throws Exception {
        String token = signup();
        long memberId = myId(token);
        long postId = createPost(token, "흡기");

        mockMvc.perform(authorized(delete("/members/me"), token)).andExpect(status().isNoContent());

        mockMvc.perform(get("/posts/" + postId)).andExpect(status().isNotFound());
        String email = jdbcTemplate.queryForObject("SELECT email FROM member WHERE id = ?", String.class, memberId);
        assertThat(email).isNull();
    }

    @Test
    void 남의_사진이나_남의_차량으로는_게시물을_올릴_수_없다() throws Exception {
        String owner = signup();
        String imageKey = upload(owner);
        long ownerCar = registerCar(owner);
        String other = signup();
        long otherCar = registerCar(other);

        createPostRequest(other, otherCar, imageKey, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        createPostRequest(other, ownerCar, upload(other), "[]")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 이미지가_아닌_파일은_게시물에_연결할_수_없다() throws Exception {
        String token = signup();
        String json = requestUploadUrl(token);
        String uploadUrl = JsonPath.read(json, "$.result.items[0].uploadUrl");
        String imageKey = JsonPath.read(json, "$.result.items[0].imageKey");
        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content("not an image".getBytes()))
                .andExpect(status().isOk());

        createPostRequest(token, registerCar(token), imageKey, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
    }

    @Test
    void 서명과_다른_Content_Type_으로는_올릴_수_없다() throws Exception {
        String uploadUrl = JsonPath.read(requestUploadUrl(signup()), "$.result.items[0].uploadUrl");

        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/webp").content(jpegBytes()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 게시물에_연결된_사진은_tmp_를_뗀_영구_키로_옮겨지고_공개_URL_로_조회된다() throws Exception {
        String token = signup();
        String uploadKey = upload(token);
        String json = createPostRequest(token, registerCar(token), uploadKey, "[]")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long postId = ((Number) JsonPath.read(json, "$.result.id")).longValue();

        String permanentKey = uploadKey.substring("tmp/".length());
        mockMvc.perform(get("/posts/" + postId))
                .andExpect(jsonPath("$.result.images[0].imageKey").value(permanentKey))
                .andExpect(jsonPath("$.result.images[0].url").value(HOST + "/images/" + permanentKey));
        mockMvc.perform(get("/images/" + permanentKey))
                .andExpect(status().isOk())
                .andExpect(content().bytes(jpegBytes()));
        mockMvc.perform(get("/images/" + uploadKey)).andExpect(status().isNotFound());
    }

    @Test
    void 게시물이_있는_보유_차량을_지우면_이전_차량으로_바뀐다() throws Exception {
        String token = signup();
        long carId = registerCar(token);
        createPostRequest(token, carId, upload(token), "[]").andExpect(status().isCreated());

        mockMvc.perform(authorized(delete("/members/me/cars/" + carId), token)).andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/members/me/cars"), token))
                .andExpect(jsonPath("$.result.items[0].id").value((int) carId))
                .andExpect(jsonPath("$.result.items[0].status").value("PAST"))
                .andExpect(jsonPath("$.result.items[0].postCount").value(1));
    }

    @Test
    void 연식이_세대_판매_기간을_벗어나면_보유_차량을_등록할_수_없다() throws Exception {
        String token = signup();

        mockMvc.perform(authorized(post("/members/me/cars"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trimId\":%d,\"year\":2015}".formatted(avanteNTrimId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MODEL_YEAR"));
    }

    // 부품 2개: 기존 브랜드(RAYS)의 새 부품(하체, 태그 있음)과 브랜드 없는 새 부품(익스테리어, 태그 없음)
    private long createPost(String token, String partName) throws Exception {
        String parts = """
                [{"ref":"p1","category":"CHASSIS","brandName":"RAYS","partName":"%s"},
                 {"ref":"p2","category":"EXTERIOR","partName":"프론트립 %s"}]
                """.formatted(partName, UUID.randomUUID().toString().substring(0, 6));
        String json = createPostRequest(token, registerCar(token), upload(token), parts)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    private ResultActions createPostRequest(String token, long carId, String imageKey, String parts) throws Exception {
        String tags = parts.contains("p1") ? "[{\"ref\":\"p1\",\"x\":0.42,\"y\":0.71}]" : "[]";
        return mockMvc.perform(authorized(post("/posts"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownedCarId":%d,"buildStyle":"DAILY","content":"데일리","parts":%s,
                         "images":[{"imageKey":"%s","width":1536,"height":2048,"tags":%s}]}
                        """.formatted(carId, parts, imageKey, tags)));
    }

    private ResultActions wish(String token, int postPartId) throws Exception {
        return mockMvc.perform(authorized(post("/wishlist"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postPartId\":%d}".formatted(postPartId)));
    }

    // 하체(RAYS) 부품의 partId
    private long partIdOf(long postId) throws Exception {
        String json = mockMvc.perform(get("/posts/" + postId)).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$.result.parts[?(@.category == 'CHASSIS')].partId");
        return ids.getFirst().longValue();
    }

    private long registerCar(String token) throws Exception {
        String json = mockMvc.perform(authorized(post("/members/me/cars"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trimId\":%d,\"year\":2023,\"nickname\":\"흰둥이\"}".formatted(avanteNTrimId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    // 업로드 URL 발급 -> 그 URL 로 파일 PUT. imageKey 를 돌려준다
    private String upload(String token) throws Exception {
        String json = requestUploadUrl(token);
        String uploadUrl = JsonPath.read(json, "$.result.items[0].uploadUrl");
        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content(jpegBytes()))
                .andExpect(status().isOk());
        return JsonPath.read(json, "$.result.items[0].imageKey");
    }

    private String requestUploadUrl(String token) throws Exception {
        return mockMvc.perform(authorized(post("/uploads/images"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1,\"contentType\":\"image/jpeg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].expiresIn").value(600))
                .andReturn().getResponse().getContentAsString();
    }

    private long avanteNId() throws Exception {
        return carModelIdByName("아반떼 N");
    }

    private long carModelIdByName(String name) throws Exception {
        String json = mockMvc.perform(get("/car-models")).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$.result.items[?(@.name == '%s')].id".formatted(name));
        return ids.getFirst().longValue();
    }

    private long avanteNTrimId() throws Exception {
        String json = mockMvc.perform(get("/car-models/" + avanteNId())).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.generations[0].trims[0].id")).longValue();
    }

    private long myId(String token) throws Exception {
        String json = mockMvc.perform(authorized(get("/members/me"), token))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    // 항상 같은 바이트가 나오도록 고정 크기·검은색 이미지를 쓴다
    private byte[] jpegBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private String signup() throws Exception {
        String email = UUID.randomUUID() + "@ijiri.com";
        postJson("/auth/email/send-code", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isNoContent());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(verificationMailClient).sendCode(eq(email), code.capture(), anyLong());
        String verified = postJson(
                "/auth/email/verify-code",
                "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code.getValue())
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String body = """
                {"email":"%s","password":"%s","nickname":"%s","verificationToken":"%s",
                 "agreements":{"age14":true,"terms":true,"privacy":true}}
                """.formatted(
                email,
                PASSWORD,
                "n" + UUID.randomUUID().toString().substring(0, 10),
                JsonPath.read(verified, "$.result.verificationToken")
        );
        String json = postJson("/auth/signup", body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.result.accessToken");
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
}

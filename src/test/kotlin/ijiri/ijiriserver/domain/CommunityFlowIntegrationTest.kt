package ijiri.ijiriserver.domain

import com.jayway.jsonpath.JsonPath
import ijiri.ijiriserver.domain.auth.email.adapter.out.external.VerificationMailClient
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

/**
 * 차종 마스터 -> 보유 차량 -> 사진 업로드 -> 게시물(부품·태그) -> 피드/상세 -> 위시리스트 -> 차단 -> 신고 -> 관리자 처리를
 * 실제 필터 체인으로 검증한다. 요청 제한이 테스트끼리 누적되지 않도록 요청마다 다른 IP 를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class CommunityFlowIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    lateinit var verificationMailClient: VerificationMailClient

    @Test
    fun 차종_마스터는_로그인_없이_조회하고_핵심_계열만_거를_수_있다() {
        mockMvc.perform(get("/car-models").param("core", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[*].isCore", not(hasItem(false))))
            .andExpect(jsonPath("$.result.items[*].name", hasItem("아반떼 N")))
        mockMvc.perform(get("/car-models").param("q", "n 라인"))
            .andExpect(jsonPath("$.result.items[0].name").value("아반떼 N 라인"))

        mockMvc.perform(get("/car-models/${avanteNId()}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.brand").value("현대"))
            .andExpect(jsonPath("$.result.generations[0].trims[0].name").value("2.0T"))
    }

    @Test
    fun 게시물을_올리면_피드와_상세에_보이고_다른_사람이_담으면_담기_수가_오른다() {
        val author = signup()
        val postId = createPost(author, "TE37 SAGA 18")

        mockMvc.perform(get("/feed").param("carModelId", avanteNId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[*].postId", hasItem(postId.toInt())))
            .andExpect(jsonPath("$.result.items[0].thumbWidth").value(1536))

        val detail = mockMvc.perform(get("/posts/$postId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.car.carModelName").value("아반떼 N"))
            .andExpect(jsonPath("$.result.car.year").value(2023))
            // 분류 순서(익스테리어, 인테리어, 파워트레인, 하체)대로
            .andExpect(jsonPath("$.result.parts[0].category").value("EXTERIOR"))
            .andExpect(jsonPath("$.result.parts[1].category").value("CHASSIS"))
            .andExpect(jsonPath("$.result.parts[1].brandName").value("RAYS"))
            .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.42))
            .andExpect(jsonPath("$.result.isMine").value(false))
            .andExpect(jsonPath("$.result.createdAt").value(endsWith("+09:00")))
            .andReturn().response.contentAsString
        val postPartId: Int = JsonPath.read(detail, "$.result.parts[1].postPartId")

        // 작성자 본인의 담기는 담기 수에 세지 않는다
        wish(author, postPartId).andExpect(status().isCreated())
        val other = signup()
        val added = wish(other, postPartId).andExpect(status().isCreated())
            .andReturn().response.contentAsString
        wish(other, postPartId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(JsonPath.read<Int>(added, "$.result.id")))

        mockMvc.perform(authorized(get("/posts/$postId"), other))
            .andExpect(jsonPath("$.result.parts[1].wishCount").value(1))
            .andExpect(jsonPath("$.result.parts[1].wished").value(true))
            .andExpect(jsonPath("$.result.wishCount").value(1))
        mockMvc.perform(authorized(get("/members/me/wishlist"), other))
            .andExpect(jsonPath("$.result.items[0].postId").value(postId.toInt()))
            .andExpect(jsonPath("$.result.items[0].partName").value("TE37 SAGA 18"))
            .andExpect(jsonPath("$.result.items[0].carModelName").value("아반떼 N"))
        mockMvc.perform(authorized(get("/members/me"), author))
            .andExpect(jsonPath("$.result.postCount").value(1))
            .andExpect(jsonPath("$.result.receivedWishCount").value(1))
    }

    @Test
    fun 대소문자_공백_하이픈만_다른_새_부품은_같은_부품이고_검색도_된다() {
        val token = signup()
        val name = "TE-37 " + UUID.randomUUID().toString().substring(0, 8)
        val first = createPost(token, name)
        val second = createPost(token, " " + name.lowercase().replace("-", "") + " ")

        assertThat(partIdOf(first)).isEqualTo(partIdOf(second))
        mockMvc.perform(authorized(get("/parts"), token).param("q", name.replace("-", "").uppercase()))
            .andExpect(jsonPath("$.result.items[0].id").value(partIdOf(first).toInt()))
            .andExpect(jsonPath("$.result.items[0].useCount").value(2))
        mockMvc.perform(authorized(get("/parts/suggest"), token).param("q", "rays $name"))
            .andExpect(jsonPath("$.result.items[0].id").value(partIdOf(first).toInt()))
            .andExpect(jsonPath("$.result.items[0].score").value(1.0))
    }

    @Test
    fun 부품을_수정해도_postPartId_를_보내면_담기가_남고_빠진_부품의_담기는_지워진다() {
        val author = signup()
        val postId = createPost(author, "브레이크 " + UUID.randomUUID().toString().substring(0, 6))
        val detail = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val kept: Int = JsonPath.read(detail, "$.result.parts[0].postPartId")
        val removed: Int = JsonPath.read(detail, "$.result.parts[1].postPartId")
        val imageKey: String = JsonPath.read(detail, "$.result.images[0].imageKey")
        val other = signup()
        wish(other, kept).andExpect(status().isCreated())
        wish(other, removed).andExpect(status().isCreated())

        mockMvc.perform(
            authorized(patch("/posts/$postId"), author)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"buildStyle":"CIRCUIT","parts":[{"ref":"a","category":"CHASSIS","postPartId":$kept}],
                         "images":[{"imageKey":"$imageKey","tags":[{"ref":"a","x":0.1,"y":0.2}]}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(authorized(get("/posts/$postId"), other))
            .andExpect(jsonPath("$.result.car.buildStyle").value("CIRCUIT"))
            .andExpect(jsonPath("$.result.parts.length()").value(1))
            .andExpect(jsonPath("$.result.parts[0].postPartId").value(kept))
            .andExpect(jsonPath("$.result.parts[0].wished").value(true))
            .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.1))
        mockMvc.perform(authorized(get("/members/me/wishlist"), other))
            .andExpect(jsonPath("$.result.items.length()").value(1))
    }

    @Test
    fun 부품_없이_태그만_보내면_수정을_거절한다() {
        val author = signup()
        val postId = createPost(author, "머플러")

        mockMvc.perform(
            authorized(patch("/posts/$postId"), author)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[{\"imageKey\":\"posts/tmp/x.png\",\"tags\":[]}]}"),
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun 게시물_작성이_검증에서_실패해도_같은_사진으로_다시_올릴_수_있다() {
        val token = signup()
        val carId = registerCar(token)
        val imageKey = upload(token)
        val badParts = "[{\"ref\":\"p1\",\"category\":\"CHASSIS\",\"partName\":\"휠\"}," +
            "{\"ref\":\"p1\",\"category\":\"CHASSIS\",\"partName\":\"타이어\"}]"

        createPostRequest(token, carId, imageKey, badParts)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PART_REF"))

        createPostRequest(token, carId, imageKey, "[]").andExpect(status().isCreated())
        mockMvc.perform(get("/images/" + imageKey.substring("tmp/".length))).andExpect(status().isOk())
    }

    @Test
    fun 회원의_게시물_목록은_로그인_없이_볼_수_있다() {
        val token = signup()
        val postId = createPost(token, "흡기 필터")

        mockMvc.perform(get("/members/${myId(token)}/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].postId").value(postId.toInt()))
    }

    @Test
    fun 같은_postPartId_를_두_번_보내면_수정을_거절한다() {
        val author = signup()
        val postId = createPost(author, "서스펜션")
        val detail = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val postPartId: Int = JsonPath.read(detail, "$.result.parts[0].postPartId")

        mockMvc.perform(
            authorized(patch("/posts/$postId"), author)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"parts":[{"ref":"a","category":"CHASSIS","postPartId":$postPartId},
                                  {"ref":"b","category":"CHASSIS","postPartId":$postPartId}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PART_REF"))
    }

    @Test
    fun 삭제된_게시물에_대한_신고도_숨김_처리로_닫을_수_있다() {
        val author = signup()
        val postId = createPost(author, "디퓨저")
        val reporter = signup()
        mockMvc.perform(
            authorized(post("/reports"), reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":$postId,\"reason\":\"SPAM\"}"),
        )
            .andExpect(status().isCreated())
        mockMvc.perform(authorized(delete("/posts/$postId"), author)).andExpect(status().isNoContent())
        val admin = signup()
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", myId(admin))
        val reportId = jdbcTemplate.queryForObject(
            "SELECT id FROM report WHERE target_type = 'POST' AND target_id = ?",
            Long::class.java,
            postId,
        )

        mockMvc.perform(
            authorized(patch("/admin/reports/$reportId"), admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"HIDE_POST\"}"),
        )
            .andExpect(status().isNoContent())
    }

    @Test
    fun 로그인_상태에서_차종을_고르지_않으면_관심_차종_피드를_본다() {
        val author = signup()
        val postId = createPost(author, "프론트립")
        val viewer = signup()
        val otherModel = carModelIdByName("M3")
        mockMvc.perform(
            authorized(put("/members/me/interest-cars"), viewer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"carModelIds\":[$otherModel]}"),
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(authorized(get("/feed"), viewer))
            .andExpect(jsonPath("$.result.items[*].postId", not(hasItem(postId.toInt()))))
        mockMvc.perform(authorized(get("/members/me/interest-cars"), viewer))
            .andExpect(jsonPath("$.result.items[0].name").value("M3"))
    }

    @Test
    fun 차단하면_서로의_게시물이_보이지_않는다() {
        val author = signup()
        val postId = createPost(author, "사이드 스커트")
        val authorId = myId(author)
        val viewer = signup()

        mockMvc.perform(authorized(post("/members/$authorId/block"), viewer))
            .andExpect(status().isNoContent())

        mockMvc.perform(authorized(get("/feed").param("carModelId", avanteNId().toString()), viewer))
            .andExpect(jsonPath("$.result.items[*].postId", not(hasItem(postId.toInt()))))
        mockMvc.perform(authorized(get("/posts/$postId"), viewer))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        mockMvc.perform(authorized(get("/members/me/blocks"), viewer))
            .andExpect(jsonPath("$.result.items[0].memberId").value(authorId.toInt()))
    }

    @Test
    fun 관리자가_신고된_게시물을_숨기면_피드에서_빠진다_역할은_다시_로그인하지_않아도_반영된다() {
        val author = signup()
        val postId = createPost(author, "스포일러")
        val reporter = signup()
        val report = "{\"targetType\":\"POST\",\"targetId\":$postId,\"reason\":\"ILLEGAL_TUNING\"}"
        repeat(2) {
            mockMvc.perform(
                authorized(post("/reports"), reporter)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(report),
            )
                .andExpect(status().isCreated())
        }

        val admin = signup()
        mockMvc.perform(authorized(get("/admin/reports"), admin))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", myId(admin))

        val reports = mockMvc.perform(authorized(get("/admin/reports"), admin))
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        val reportIds: List<Int> = JsonPath.read(reports, "$.result.items[?(@.targetId == $postId)].id")
        val counts: List<Int> = JsonPath.read(
            reports,
            "$.result.items[?(@.targetId == $postId)].reportCount",
        )
        assertThat(counts).containsExactly(1)

        mockMvc.perform(
            authorized(patch("/admin/reports/${reportIds.first()}"), admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"HIDE_POST\"}"),
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(get("/posts/$postId")).andExpect(status().isNotFound())
        mockMvc.perform(authorized(get("/members/${myId(author)}/posts"), author))
            .andExpect(jsonPath("$.result.items[0].hidden").value(true))
    }

    @Test
    fun 탈퇴하면_게시물이_비공개되고_이메일이_지워진다() {
        val token = signup()
        val memberId = myId(token)
        val postId = createPost(token, "흡기")

        mockMvc.perform(authorized(delete("/members/me"), token)).andExpect(status().isNoContent())

        mockMvc.perform(get("/posts/$postId")).andExpect(status().isNotFound())
        val email = jdbcTemplate.queryForObject(
            "SELECT email FROM member WHERE id = ?",
            String::class.java,
            memberId,
        )
        assertThat(email).isNull()
    }

    @Test
    fun 남의_사진이나_남의_차량으로는_게시물을_올릴_수_없다() {
        val owner = signup()
        val imageKey = upload(owner)
        val ownerCar = registerCar(owner)
        val other = signup()
        val otherCar = registerCar(other)

        createPostRequest(other, otherCar, imageKey, "[]")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
        createPostRequest(other, ownerCar, upload(other), "[]")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
    }

    @Test
    fun 이미지가_아닌_파일은_게시물에_연결할_수_없다() {
        val token = signup()
        val json = requestUploadUrl(token)
        val uploadUrl: String = JsonPath.read(json, "$.result.items[0].uploadUrl")
        val imageKey: String = JsonPath.read(json, "$.result.items[0].imageKey")
        mockMvc.perform(
            put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content("not an image".toByteArray()),
        )
            .andExpect(status().isOk())

        createPostRequest(token, registerCar(token), imageKey, "[]")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
    }

    @Test
    fun 서명과_다른_Content_Type_으로는_올릴_수_없다() {
        val uploadUrl: String = JsonPath.read(requestUploadUrl(signup()), "$.result.items[0].uploadUrl")

        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/webp").content(jpegBytes()))
            .andExpect(status().isForbidden())
    }

    @Test
    fun 게시물에_연결된_사진은_tmp_를_뗀_영구_키로_옮겨지고_공개_URL_로_조회된다() {
        val token = signup()
        val uploadKey = upload(token)
        val json = createPostRequest(token, registerCar(token), uploadKey, "[]")
            .andExpect(status().isCreated())
            .andReturn().response.contentAsString
        val postId = JsonPath.read<Number>(json, "$.result.id").toLong()

        val permanentKey = uploadKey.substring("tmp/".length)
        mockMvc.perform(get("/posts/$postId"))
            .andExpect(jsonPath("$.result.images[0].imageKey").value(permanentKey))
            .andExpect(jsonPath("$.result.images[0].url").value("$HOST/images/$permanentKey"))
        mockMvc.perform(get("/images/$permanentKey"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(jpegBytes()))
        mockMvc.perform(get("/images/$uploadKey")).andExpect(status().isNotFound())
    }

    @Test
    fun 게시물이_있는_보유_차량을_지우면_이전_차량으로_바뀐다() {
        val token = signup()
        val carId = registerCar(token)
        createPostRequest(token, carId, upload(token), "[]").andExpect(status().isCreated())

        mockMvc.perform(authorized(delete("/members/me/cars/$carId"), token)).andExpect(status().isNoContent())

        mockMvc.perform(authorized(get("/members/me/cars"), token))
            .andExpect(jsonPath("$.result.items[0].id").value(carId.toInt()))
            .andExpect(jsonPath("$.result.items[0].status").value("PAST"))
            .andExpect(jsonPath("$.result.items[0].postCount").value(1))
    }

    @Test
    fun 연식이_세대_판매_기간을_벗어나면_보유_차량을_등록할_수_없다() {
        val token = signup()

        mockMvc.perform(
            authorized(post("/members/me/cars"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trimId\":${avanteNTrimId()},\"year\":2015}"),
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_MODEL_YEAR"))
    }

    // 부품 2개: 기존 브랜드(RAYS)의 새 부품(하체, 태그 있음)과 브랜드 없는 새 부품(익스테리어, 태그 없음)
    private fun createPost(token: String, partName: String): Long {
        val parts = """
            [{"ref":"p1","category":"CHASSIS","brandName":"RAYS","partName":"$partName"},
             {"ref":"p2","category":"EXTERIOR","partName":"프론트립 ${UUID.randomUUID().toString().substring(0, 6)}"}]
        """.trimIndent()
        val json = createPostRequest(token, registerCar(token), upload(token), parts)
            .andExpect(status().isCreated())
            .andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.id").toLong()
    }

    private fun createPostRequest(token: String, carId: Long, imageKey: String, parts: String): ResultActions {
        val tags = if (parts.contains("p1")) "[{\"ref\":\"p1\",\"x\":0.42,\"y\":0.71}]" else "[]"
        return mockMvc.perform(
            authorized(post("/posts"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"ownedCarId":$carId,"buildStyle":"DAILY","content":"데일리","parts":$parts,
                         "images":[{"imageKey":"$imageKey","width":1536,"height":2048,"tags":$tags}]}
                    """.trimIndent(),
                ),
        )
    }

    private fun wish(token: String, postPartId: Int): ResultActions =
        mockMvc.perform(
            authorized(post("/wishlist"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postPartId\":$postPartId}"),
        )

    // 하체(RAYS) 부품의 partId
    private fun partIdOf(postId: Long): Long {
        val json = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val ids: List<Number> = JsonPath.read(json, "$.result.parts[?(@.category == 'CHASSIS')].partId")
        return ids.first().toLong()
    }

    private fun registerCar(token: String): Long {
        val json = mockMvc.perform(
            authorized(post("/members/me/cars"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trimId\":${avanteNTrimId()},\"year\":2023,\"nickname\":\"흰둥이\"}"),
        )
            .andExpect(status().isCreated())
            .andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.id").toLong()
    }

    // 업로드 URL 발급 -> 그 URL 로 파일 PUT. imageKey 를 돌려준다
    private fun upload(token: String): String {
        val json = requestUploadUrl(token)
        val uploadUrl: String = JsonPath.read(json, "$.result.items[0].uploadUrl")
        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content(jpegBytes()))
            .andExpect(status().isOk())
        return JsonPath.read(json, "$.result.items[0].imageKey")
    }

    private fun requestUploadUrl(token: String): String =
        mockMvc.perform(
            authorized(post("/uploads/images"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\":1,\"contentType\":\"image/jpeg\"}"),
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].expiresIn").value(600))
            .andReturn().response.contentAsString

    private fun avanteNId(): Long = carModelIdByName("아반떼 N")

    private fun carModelIdByName(name: String): Long {
        val json = mockMvc.perform(get("/car-models")).andReturn().response.contentAsString
        val ids: List<Number> = JsonPath.read(json, "$.result.items[?(@.name == '$name')].id")
        return ids.first().toLong()
    }

    private fun avanteNTrimId(): Long {
        val json = mockMvc.perform(get("/car-models/${avanteNId()}")).andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.generations[0].trims[0].id").toLong()
    }

    private fun myId(token: String): Long {
        val json = mockMvc.perform(authorized(get("/members/me"), token))
            .andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.id").toLong()
    }

    // 항상 같은 바이트가 나오도록 고정 크기·검은색 이미지를 쓴다
    private fun jpegBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpg", out)
        return out.toByteArray()
    }

    // 인증 코드 발송 -> 확인 -> 가입. 메일 발송은 목으로 대신하고 발송된 코드를 가로챈다
    private fun signup(): String {
        val email = "${UUID.randomUUID()}@ijiri.com"
        postJson("/auth/email/send-code", "{\"email\":\"$email\"}")
            .andExpect(status().isNoContent())
        val code = argumentCaptor<String>()
        verify(verificationMailClient).sendCode(eq(email), code.capture(), any())
        val verified = postJson(
            "/auth/email/verify-code",
            "{\"email\":\"$email\",\"code\":\"${code.lastValue}\"}",
        ).andExpect(status().isOk()).andReturn().response.contentAsString
        val nickname = "n" + UUID.randomUUID().toString().substring(0, 10)
        val verificationToken: String = JsonPath.read(verified, "$.result.verificationToken")
        val body = """
            {"email":"$email","password":"$PASSWORD","nickname":"$nickname","verificationToken":"$verificationToken",
             "agreements":{"age14":true,"terms":true,"privacy":true}}
        """.trimIndent()
        val json = postJson("/auth/signup", body)
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        return JsonPath.read(json, "$.result.accessToken")
    }

    private fun postJson(url: String, body: String): ResultActions =
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).with(uniqueIp()))

    private fun authorized(builder: MockHttpServletRequestBuilder, token: String): MockHttpServletRequestBuilder =
        builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token").with(uniqueIp())

    private fun uniqueIp(): RequestPostProcessor =
        RequestPostProcessor { request ->
            val sequence = IP_SEQUENCE.getAndIncrement()
            request.remoteAddr = "10.1.${sequence / 250}.${sequence % 250}"
            request
        }

    companion object {
        private const val PASSWORD = "abcd1234"
        private const val HOST = "http://localhost"
        private val IP_SEQUENCE = AtomicInteger()
    }
}

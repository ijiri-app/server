package ijiri.ijiriserver.community

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.common.contract.MemberSummary
import ijiri.ijiriserver.common.contract.OwnedCarSnapshot
import ijiri.ijiriserver.common.contract.PartInfo
import ijiri.ijiriserver.common.contract.PartResolveRequest
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.exception.RemoteStatusCode
import ijiri.ijiriserver.common.messaging.MemberPurgedMessage
import ijiri.ijiriserver.common.messaging.MemberWithdrawnMessage
import ijiri.ijiriserver.common.messaging.MessagePublisher
import ijiri.ijiriserver.common.messaging.Topics
import ijiri.ijiriserver.common.security.GatewayHeaders
import ijiri.ijiriserver.community.messaging.MemberEventConsumer
import ijiri.ijiriserver.community.post.adapter.out.external.CarServiceClient
import ijiri.ijiriserver.community.post.adapter.out.external.MediaServiceClient
import ijiri.ijiriserver.community.post.adapter.out.external.UserServiceClient
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 게시물(부품·태그) -> 피드/상세 -> 위시리스트 -> 차단 -> 신고 -> 관리자 처리 -> 회원 이벤트를 실제 필터 체인으로 검증한다.
 * 게이트웨이가 넣는 X-Member-Id / X-Member-Role 은 헤더로 직접 넣고, 다른 서비스(회원·보유 차량·차종·부품·사진)와 Kafka 는 목이다.
 * 보유 차량 id 는 회원 id * 10 + n 으로 두어 남의 차량을 흉내 낸다.
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

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberEventConsumer: MemberEventConsumer

    @MockitoBean
    lateinit var userServiceClient: UserServiceClient

    @MockitoBean
    lateinit var carServiceClient: CarServiceClient

    @MockitoBean
    lateinit var mediaServiceClient: MediaServiceClient

    @MockitoBean
    lateinit var messagePublisher: MessagePublisher

    @BeforeEach
    fun stubOtherServices() {
        whenever(userServiceClient.findActive(any())).thenAnswer { invocation ->
            val id = invocation.getArgument<Long>(0)
            MemberSummary(id, "회원$id", null, null)
        }
        whenever(userServiceClient.getActive(any())).thenAnswer { invocation ->
            invocation.getArgument<Collection<Long>>(0).map { MemberSummary(it, "회원$it", null, null) }
        }
        whenever(userServiceClient.getSnapshot(any(), any())).thenAnswer { invocation ->
            val memberId = invocation.getArgument<Long>(0)
            val ownedCarId = invocation.getArgument<Long>(1)
            if (ownedCarId / 10 != memberId) {
                throw CustomException(RemoteStatusCode(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 차량만 사용할 수 있습니다."))
            }
            OwnedCarSnapshot(ownedCarId, memberId, AVANTE_N, 2023)
        }
        whenever(carServiceClient.getSpecByTrim(any())).thenReturn(AVANTE_N)
        whenever(carServiceClient.getModelNames(any())).thenAnswer { invocation ->
            invocation.getArgument<Collection<Long>>(0).associateWith { if (it == M3_ID) "M3" else "아반떼 N" }
        }
        whenever(carServiceClient.resolve(any())).thenAnswer { invocation -> resolvePart(invocation.getArgument(0)) }
        whenever(carServiceClient.getPartInfos(any())).thenAnswer { invocation ->
            val ids = invocation.getArgument<Collection<Long>>(0).toSet()
            PARTS.values.filter { it.id in ids }.associateBy { it.id }
        }
        whenever(mediaServiceClient.attach(any(), any())).thenAnswer { invocation ->
            invocation.getArgument<List<String>>(1).map { key ->
                val permanentKey = key.removePrefix("tmp/")
                AttachedImage(permanentKey, "$IMAGE_HOST/$permanentKey")
            }
        }
    }

    @Test
    fun 게시물을_올리면_피드와_상세에_보이고_다른_사람이_담으면_담기_수가_오른다() {
        val author = newMemberId()
        val postId = createPost(author, "TE37 SAGA 18")

        mockMvc.perform(get("/feed").param("carModelId", AVANTE_N_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[*].postId", hasItem(postId.toInt())))
            .andExpect(jsonPath("$.result.items[0].thumbWidth").value(1536))
            .andExpect(jsonPath("$.result.items[0].carModelName").value("아반떼 N"))

        val detail = mockMvc.perform(get("/posts/$postId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.author.nickname").value("회원$author"))
            .andExpect(jsonPath("$.result.car.carModelName").value("아반떼 N"))
            .andExpect(jsonPath("$.result.car.year").value(2023))
            // 분류 순서(익스테리어, 인테리어, 파워트레인, 하체)대로
            .andExpect(jsonPath("$.result.parts[0].category").value("EXTERIOR"))
            .andExpect(jsonPath("$.result.parts[1].category").value("CHASSIS"))
            .andExpect(jsonPath("$.result.parts[1].brandName").value("RAYS"))
            .andExpect(jsonPath("$.result.images[0].url").value(endsWith(".jpg")))
            .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.42))
            .andExpect(jsonPath("$.result.isMine").value(false))
            .andExpect(jsonPath("$.result.createdAt").value(endsWith("+09:00")))
            .andReturn().response.contentAsString
        val postPartId: Int = JsonPath.read(detail, "$.result.parts[1].postPartId")

        // 작성자 본인의 담기는 담기 수에 세지 않는다
        wish(author, postPartId).andExpect(status().isCreated())
        val other = newMemberId()
        val added = wish(other, postPartId).andExpect(status().isCreated())
            .andReturn().response.contentAsString
        wish(other, postPartId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(JsonPath.read<Int>(added, "$.result.id")))

        mockMvc.perform(asMember(get("/posts/$postId"), other))
            .andExpect(jsonPath("$.result.parts[1].wishCount").value(1))
            .andExpect(jsonPath("$.result.parts[1].wished").value(true))
            .andExpect(jsonPath("$.result.wishCount").value(1))
        mockMvc.perform(asMember(get("/members/me/wishlist"), other))
            .andExpect(jsonPath("$.result.items[0].postId").value(postId.toInt()))
            .andExpect(jsonPath("$.result.items[0].partName").value("TE37 SAGA 18"))
            .andExpect(jsonPath("$.result.items[0].carModelName").value("아반떼 N"))
        // service-user 의 마이 페이지가 부르는 내부 API
        mockMvc.perform(get("/internal/members/$author/counts"))
            .andExpect(jsonPath("$.result.postCount").value(1))
            .andExpect(jsonPath("$.result.receivedWishCount").value(1))
        mockMvc.perform(get("/internal/owned-cars/post-counts").param("ids", "${ownedCarOf(author)},999999"))
            .andExpect(jsonPath("$.result.${ownedCarOf(author)}").value(1))
            .andExpect(jsonPath("$.result.999999").doesNotExist())
    }

    @Test
    fun 부품을_수정해도_postPartId_를_보내면_담기가_남고_빠진_부품의_담기는_지워진다() {
        val author = newMemberId()
        val postId = createPost(author, "브레이크 " + UUID.randomUUID().toString().substring(0, 6))
        val detail = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val kept: Int = JsonPath.read(detail, "$.result.parts[0].postPartId")
        val removed: Int = JsonPath.read(detail, "$.result.parts[1].postPartId")
        val imageKey: String = JsonPath.read(detail, "$.result.images[0].imageKey")
        val other = newMemberId()
        wish(other, kept).andExpect(status().isCreated())
        wish(other, removed).andExpect(status().isCreated())

        mockMvc.perform(
            asMember(patch("/posts/$postId"), author)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"buildStyle":"CIRCUIT","parts":[{"ref":"a","category":"CHASSIS","postPartId":$kept}],
                         "images":[{"imageKey":"$imageKey","tags":[{"ref":"a","x":0.1,"y":0.2}]}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(asMember(get("/posts/$postId"), other))
            .andExpect(jsonPath("$.result.car.buildStyle").value("CIRCUIT"))
            .andExpect(jsonPath("$.result.parts.length()").value(1))
            .andExpect(jsonPath("$.result.parts[0].postPartId").value(kept))
            .andExpect(jsonPath("$.result.parts[0].wished").value(true))
            .andExpect(jsonPath("$.result.images[0].tags[0].x").value(0.1))
        mockMvc.perform(asMember(get("/members/me/wishlist"), other))
            .andExpect(jsonPath("$.result.items.length()").value(1))
        // 작성 때 한 번, 빠진 부품의 사용 수가 줄도록 수정 때 한 번 service-car 에 알린다
        verify(messagePublisher, times(2)).publish(eq(Topics.PART_USAGE_CHANGED), eq(AVANTE_N_ID.toString()), any())
    }

    @Test
    fun 부품_없이_태그만_보내면_수정을_거절한다() {
        val author = newMemberId()
        val postId = createPost(author, "머플러")

        mockMvc.perform(
            asMember(patch("/posts/$postId"), author)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[{\"imageKey\":\"posts/x.jpg\",\"tags\":[]}]}"),
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun 같은_postPartId_를_두_번_보내면_수정을_거절한다() {
        val author = newMemberId()
        val postId = createPost(author, "서스펜션")
        val detail = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val postPartId: Int = JsonPath.read(detail, "$.result.parts[0].postPartId")

        mockMvc.perform(
            asMember(patch("/posts/$postId"), author)
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
    fun 회원의_게시물_목록은_로그인_없이_볼_수_있다() {
        val author = newMemberId()
        val postId = createPost(author, "흡기 필터")

        mockMvc.perform(get("/members/$author/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].postId").value(postId.toInt()))
    }

    @Test
    fun 남의_차량이나_남의_사진으로는_게시물을_올릴_수_없다() {
        val me = newMemberId()
        val other = newMemberId()

        createPostRequest(me, ownedCarOf(other), uploadKey(), "[]")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))

        // service-media 가 남의 사진이라고 거절한 응답을 그대로 전달한다
        whenever(mediaServiceClient.attach(eq(me), any()))
            .thenThrow(CustomException(RemoteStatusCode(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "사진을 찾을 수 없습니다.")))
        createPostRequest(me, ownedCarOf(me), uploadKey(), "[]")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
    }

    @Test
    fun 게시물_작성이_검증에서_실패하면_옮겨진_사진을_service_media_에서_지운다() {
        val me = newMemberId()
        val imageKey = uploadKey()
        val badParts = "[{\"ref\":\"p1\",\"category\":\"CHASSIS\",\"partName\":\"휠\"}," +
            "{\"ref\":\"p1\",\"category\":\"CHASSIS\",\"partName\":\"타이어\"}]"

        createPostRequest(me, ownedCarOf(me), imageKey, badParts)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PART_REF"))

        verify(mediaServiceClient).delete(me, listOf(imageKey.removePrefix("tmp/")))
        verify(messagePublisher, never()).publish(eq(Topics.PART_USAGE_CHANGED), any(), any())
    }

    @Test
    fun 로그인_상태에서_차종을_고르지_않으면_관심_차종_피드를_본다() {
        val author = newMemberId()
        val postId = createPost(author, "프론트립")
        val viewer = newMemberId()
        whenever(userServiceClient.getCarModelIds(viewer)).thenReturn(listOf(M3_ID))

        mockMvc.perform(asMember(get("/feed"), viewer))
            .andExpect(jsonPath("$.result.items[*].postId", not(hasItem(postId.toInt()))))
        mockMvc.perform(get("/feed"))
            .andExpect(jsonPath("$.result.items[*].postId", hasItem(postId.toInt())))
    }

    @Test
    fun 차단하면_서로의_게시물이_보이지_않는다() {
        val author = newMemberId()
        val postId = createPost(author, "사이드 스커트")
        val viewer = newMemberId()

        mockMvc.perform(asMember(post("/members/$author/block"), viewer))
            .andExpect(status().isNoContent())

        mockMvc.perform(asMember(get("/feed").param("carModelId", AVANTE_N_ID.toString()), viewer))
            .andExpect(jsonPath("$.result.items[*].postId", not(hasItem(postId.toInt()))))
        mockMvc.perform(asMember(get("/posts/$postId"), viewer))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        mockMvc.perform(asMember(get("/members/me/blocks"), viewer))
            .andExpect(jsonPath("$.result.items[0].memberId").value(author.toInt()))
            .andExpect(jsonPath("$.result.items[0].nickname").value("회원$author"))
    }

    @Test
    fun 관리자가_신고된_게시물을_숨기면_피드에서_빠진다_역할은_게이트웨이_헤더로_온다() {
        val author = newMemberId()
        val postId = createPost(author, "스포일러")
        val reporter = newMemberId()
        val report = "{\"targetType\":\"POST\",\"targetId\":$postId,\"reason\":\"ILLEGAL_TUNING\"}"
        repeat(2) {
            mockMvc.perform(
                asMember(post("/reports"), reporter)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(report),
            )
                .andExpect(status().isCreated())
        }

        val admin = newMemberId()
        mockMvc.perform(asMember(get("/admin/reports"), admin))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))

        val reports = mockMvc.perform(asAdmin(get("/admin/reports"), admin))
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        val reportIds: List<Int> = JsonPath.read(reports, "$.result.items[?(@.targetId == $postId)].id")
        val counts: List<Int> = JsonPath.read(reports, "$.result.items[?(@.targetId == $postId)].reportCount")
        assertThat(counts).containsExactly(1)

        mockMvc.perform(
            asAdmin(patch("/admin/reports/${reportIds.first()}"), admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"HIDE_POST\"}"),
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(get("/posts/$postId")).andExpect(status().isNotFound())
        mockMvc.perform(asMember(get("/members/$author/posts"), author))
            .andExpect(jsonPath("$.result.items[0].hidden").value(true))
    }

    @Test
    fun 삭제된_게시물에_대한_신고도_숨김_처리로_닫을_수_있고_삭제는_사진_삭제_이벤트를_발행한다() {
        val author = newMemberId()
        val postId = createPost(author, "디퓨저")
        val reporter = newMemberId()
        mockMvc.perform(
            asMember(post("/reports"), reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"POST\",\"targetId\":$postId,\"reason\":\"SPAM\"}"),
        )
            .andExpect(status().isCreated())

        mockMvc.perform(asMember(delete("/posts/$postId"), author)).andExpect(status().isNoContent())
        verify(messagePublisher).publish(eq(Topics.POST_DELETED), eq(postId.toString()), any())

        val reportId = jdbcTemplate.queryForObject(
            "SELECT id FROM report WHERE target_type = 'POST' AND target_id = ?",
            Long::class.java,
            postId,
        )
        mockMvc.perform(
            asAdmin(patch("/admin/reports/$reportId"), newMemberId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"HIDE_POST\"}"),
        )
            .andExpect(status().isNoContent())
    }

    @Test
    fun 회원_신고를_정지로_처리하면_service_user_에_정지를_요청한다() {
        val target = newMemberId()
        val reporter = newMemberId()
        mockMvc.perform(
            asMember(post("/reports"), reporter)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"MEMBER\",\"targetId\":$target,\"reason\":\"INAPPROPRIATE\"}"),
        )
            .andExpect(status().isCreated())
        val reportId = jdbcTemplate.queryForObject(
            "SELECT id FROM report WHERE target_type = 'MEMBER' AND target_id = ?",
            Long::class.java,
            target,
        )

        mockMvc.perform(
            asAdmin(patch("/admin/reports/$reportId"), newMemberId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUSPEND_MEMBER\",\"suspendDays\":3}"),
        )
            .andExpect(status().isNoContent())

        verify(userServiceClient).suspend(eq(target), any())
    }

    @Test
    fun 탈퇴_이벤트를_받으면_게시물이_비공개되고_영구_삭제_이벤트를_받으면_데이터가_지워진다() {
        val author = newMemberId()
        val postId = createPost(author, "흡기")
        val detail = mockMvc.perform(get("/posts/$postId")).andReturn().response.contentAsString
        val postPartId: Int = JsonPath.read(detail, "$.result.parts[0].postPartId")
        val fan = newMemberId()
        wish(fan, postPartId).andExpect(status().isCreated())
        mockMvc.perform(asMember(post("/members/$author/block"), fan)).andExpect(status().isNoContent())

        memberEventConsumer.onMemberWithdrawn(objectMapper.writeValueAsString(MemberWithdrawnMessage(author)))
        mockMvc.perform(get("/posts/$postId")).andExpect(status().isNotFound())
        mockMvc.perform(asMember(get("/members/me/wishlist"), fan))
            .andExpect(jsonPath("$.result.items.length()").value(0))

        memberEventConsumer.onMemberPurged(objectMapper.writeValueAsString(MemberPurgedMessage(author)))
        assertThat(countRows("post", "member_id", author)).isZero()
        assertThat(countRows("wishlist_item", "post_id", postId)).isZero()
        assertThat(countRows("member_block", "blocked_id", author)).isZero()
        verify(messagePublisher).publish(eq(Topics.POST_DELETED), eq(postId.toString()), any())
        // 같은 메시지를 다시 받아도 안전하다
        memberEventConsumer.onMemberPurged(objectMapper.writeValueAsString(MemberPurgedMessage(author)))
    }

    private fun countRows(table: String, column: String, value: Long): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE $column = ?", Int::class.java, value)!!

    // 부품 2개: 기존 브랜드(RAYS)의 새 부품(하체, 태그 있음)과 브랜드 없는 새 부품(익스테리어, 태그 없음)
    private fun createPost(memberId: Long, partName: String): Long {
        val parts = """
            [{"ref":"p1","category":"CHASSIS","brandName":"RAYS","partName":"$partName"},
             {"ref":"p2","category":"EXTERIOR","partName":"프론트립 ${UUID.randomUUID().toString().substring(0, 6)}"}]
        """.trimIndent()
        val json = createPostRequest(memberId, ownedCarOf(memberId), uploadKey(), parts)
            .andExpect(status().isCreated())
            .andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.id").toLong()
    }

    private fun createPostRequest(memberId: Long, carId: Long, imageKey: String, parts: String): ResultActions {
        val tags = if (parts.contains("p1")) "[{\"ref\":\"p1\",\"x\":0.42,\"y\":0.71}]" else "[]"
        return mockMvc.perform(
            asMember(post("/posts"), memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"ownedCarId":$carId,"buildStyle":"DAILY","content":"데일리","parts":$parts,
                         "images":[{"imageKey":"$imageKey","width":1536,"height":2048,"tags":$tags}]}
                    """.trimIndent(),
                ),
        )
    }

    private fun wish(memberId: Long, postPartId: Int): ResultActions =
        mockMvc.perform(
            asMember(post("/wishlist"), memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postPartId\":$postPartId}"),
        )

    // 게이트웨이가 넣는 헤더
    private fun asMember(builder: MockHttpServletRequestBuilder, memberId: Long): MockHttpServletRequestBuilder =
        builder.header(GatewayHeaders.MEMBER_ID, memberId.toString()).header(GatewayHeaders.MEMBER_ROLE, "USER")

    private fun asAdmin(builder: MockHttpServletRequestBuilder, memberId: Long): MockHttpServletRequestBuilder =
        builder.header(GatewayHeaders.MEMBER_ID, memberId.toString()).header(GatewayHeaders.MEMBER_ROLE, "ADMIN")

    private fun ownedCarOf(memberId: Long): Long = memberId * 10 + 1

    private fun uploadKey(): String = "tmp/posts/${UUID.randomUUID()}.jpg"

    private fun newMemberId(): Long = MEMBER_ID_SEQUENCE.incrementAndGet()

    // service-car 의 부품 등록을 흉내 낸다. 브랜드와 이름이 같으면 같은 부품, partId 가 있으면 그 부품
    private fun resolvePart(request: PartResolveRequest): PartInfo {
        request.partId?.let { id -> return PARTS.values.first { it.id == id } }
        val key = "${request.brandName.orEmpty()}|${request.partName!!.trim()}"
        return PARTS.computeIfAbsent(key) {
            PartInfo(
                PART_ID_SEQUENCE.incrementAndGet(),
                request.partName!!.trim(),
                request.category,
                if (request.brandName == null) null else 1L,
                request.brandName,
            )
        }
    }

    companion object {
        private const val IMAGE_HOST = "http://localhost/images"
        private const val AVANTE_N_ID = 1L
        private const val M3_ID = 2L
        private val AVANTE_N = CarSpec(AVANTE_N_ID, "현대", "아반떼 N", 1L, "CN7", 2021, null, 1L, "2.0T")
        private val MEMBER_ID_SEQUENCE = AtomicLong(100)
        private val PART_ID_SEQUENCE = AtomicLong(0)
        private val PARTS = ConcurrentHashMap<String, PartInfo>()
    }
}

package ijiri.ijiriserver.media

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import ijiri.ijiriserver.common.messaging.MemberPurgedMessage
import ijiri.ijiriserver.common.messaging.PostDeletedMessage
import ijiri.ijiriserver.common.security.GatewayHeaders
import ijiri.ijiriserver.media.upload.adapter.`in`.messaging.UploadEventConsumer
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

/**
 * 업로드 URL 발급 -> 파일 PUT -> 다른 서비스의 연결(attach) -> 공개 URL 조회 -> 삭제(REST, 이벤트)를 실제 필터 체인으로 검증한다.
 * 회원별 업로드 제한이 테스트끼리 누적되지 않도록 요청마다 다른 회원 id 를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class MediaFlowIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var uploadEventConsumer: UploadEventConsumer

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun 연결된_사진은_tmp_를_뗀_영구_키로_옮겨지고_공개_URL_로_조회된다() {
        val memberId = newMemberId()
        val uploadKey = upload(memberId)

        val json = attach(memberId, uploadKey, "POST")
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        val permanentKey = uploadKey.substring("tmp/".length)
        val url: String = JsonPath.read(json, "$.result[0].url")

        mockMvc.perform(get("/images/$permanentKey"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(jpegBytes()))
        mockMvc.perform(get("/images/$uploadKey")).andExpect(status().isNotFound())
        // 같은 키를 다시 연결할 수 없다
        attach(memberId, uploadKey, "POST").andExpect(status().isBadRequest())
        // 영구 키로도 다시 연결할 수 없다
        attach(memberId, permanentKey, "POST")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
        // url 은 공개 주소 + 영구 키
        org.assertj.core.api.Assertions.assertThat(url).isEqualTo("$HOST/images/$permanentKey")
    }

    @Test
    fun 남의_사진이나_다른_용도의_키는_연결할_수_없다() {
        val owner = newMemberId()
        val postKey = upload(owner)

        attach(newMemberId(), postKey, "POST")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
        attach(owner, postKey, "PROFILE")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
    }

    @Test
    fun 이미지가_아닌_파일은_연결할_수_없다() {
        val memberId = newMemberId()
        val json = requestUploadUrl(memberId, "POST")
        val uploadUrl: String = JsonPath.read(json, "$.result.items[0].uploadUrl")
        val imageKey: String = JsonPath.read(json, "$.result.items[0].imageKey")
        mockMvc.perform(
            put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content("not an image".toByteArray()),
        )
            .andExpect(status().isOk())

        attach(memberId, imageKey, "POST")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
    }

    @Test
    fun 서명과_다른_Content_Type_으로는_올릴_수_없다() {
        val uploadUrl: String = JsonPath.read(requestUploadUrl(newMemberId(), "POST"), "$.result.items[0].uploadUrl")

        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/webp").content(jpegBytes()))
            .andExpect(status().isForbidden())
    }

    @Test
    fun 업로드_URL_발급은_로그인이_필요하다() {
        mockMvc.perform(
            post("/uploads/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\":1,\"contentType\":\"image/jpeg\"}"),
        )
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun 프로필_사진은_공개_URL_로_지우고_남의_사진은_무시한다() {
        val memberId = newMemberId()
        val uploadKey = upload(memberId, "PROFILE")
        val json = attach(memberId, uploadKey, "PROFILE")
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        val url: String = JsonPath.read(json, "$.result[0].url")
        val permanentKey: String = JsonPath.read(json, "$.result[0].imageKey")

        deleteByUrls(newMemberId(), url).andExpect(status().isNoContent())
        mockMvc.perform(get("/images/$permanentKey")).andExpect(status().isOk())

        deleteByUrls(memberId, url, "https://k.kakaocdn.net/profile.jpg").andExpect(status().isNoContent())
        mockMvc.perform(get("/images/$permanentKey")).andExpect(status().isNotFound())
    }

    @Test
    fun 게시물_삭제와_회원_영구_삭제_이벤트로_사진이_지워진다() {
        val memberId = newMemberId()
        val postImage = attachedKey(memberId, upload(memberId), "POST")
        val profileImage = attachedKey(memberId, upload(memberId, "PROFILE"), "PROFILE")

        uploadEventConsumer.onPostDeleted(objectMapper.writeValueAsString(PostDeletedMessage(1L, listOf(postImage))))
        mockMvc.perform(get("/images/$postImage")).andExpect(status().isNotFound())
        mockMvc.perform(get("/images/$profileImage")).andExpect(status().isOk())

        uploadEventConsumer.onMemberPurged(objectMapper.writeValueAsString(MemberPurgedMessage(memberId)))
        mockMvc.perform(get("/images/$profileImage")).andExpect(status().isNotFound())
        // 다시 받아도 안전하다
        uploadEventConsumer.onMemberPurged(objectMapper.writeValueAsString(MemberPurgedMessage(memberId)))
    }

    private fun attachedKey(memberId: Long, uploadKey: String, purpose: String): String {
        val json = attach(memberId, uploadKey, purpose).andExpect(status().isOk()).andReturn().response.contentAsString
        return JsonPath.read(json, "$.result[0].imageKey")
    }

    private fun attach(memberId: Long, imageKey: String, purpose: String): ResultActions =
        mockMvc.perform(
            post("/internal/images/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":$memberId,\"imageKeys\":[\"$imageKey\"],\"purpose\":\"$purpose\"}"),
        )

    private fun deleteByUrls(memberId: Long, vararg urls: String): ResultActions =
        mockMvc.perform(
            post("/internal/images/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":$memberId,\"imageUrls\":[${urls.joinToString(",") { "\"$it\"" }}]}"),
        )

    // 업로드 URL 발급 -> 그 URL 로 파일 PUT. imageKey 를 돌려준다
    private fun upload(memberId: Long, purpose: String = "POST"): String {
        val json = requestUploadUrl(memberId, purpose)
        val uploadUrl: String = JsonPath.read(json, "$.result.items[0].uploadUrl")
        mockMvc.perform(put(uploadUrl.replace(HOST, "")).contentType("image/jpeg").content(jpegBytes()))
            .andExpect(status().isOk())
        return JsonPath.read(json, "$.result.items[0].imageKey")
    }

    private fun requestUploadUrl(memberId: Long, purpose: String): String =
        mockMvc.perform(
            post("/uploads/images")
                .header(GatewayHeaders.MEMBER_ID, memberId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\":1,\"contentType\":\"image/jpeg\",\"purpose\":\"$purpose\"}"),
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].expiresIn").value(600))
            .andReturn().response.contentAsString

    // 항상 같은 바이트가 나오도록 고정 크기·검은색 이미지를 쓴다
    private fun jpegBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpg", out)
        return out.toByteArray()
    }

    private fun newMemberId(): Long = MEMBER_ID_SEQUENCE.incrementAndGet()

    companion object {
        private const val HOST = "http://localhost"
        private val MEMBER_ID_SEQUENCE = AtomicLong(1000)
    }
}

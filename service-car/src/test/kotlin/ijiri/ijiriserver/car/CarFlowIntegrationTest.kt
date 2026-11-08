package ijiri.ijiriserver.car

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import ijiri.ijiriserver.car.part.adapter.`in`.messaging.PartUsageConsumer
import ijiri.ijiriserver.common.messaging.PartUsageChangedMessage
import ijiri.ijiriserver.common.security.GatewayHeaders
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * 차종 마스터 조회, 부품 등록·검색·추천, 다른 서비스가 부르는 내부 API, 부품 사용 수 이벤트를 실제 필터 체인으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class CarFlowIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var partUsageConsumer: PartUsageConsumer

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun 배포_스크립트의_readiness_확인은_로그인_없이_UP_이다() {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isUnauthorized())
    }

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
    fun 부품_검색은_로그인이_필요하다() {
        mockMvc.perform(get("/parts").param("q", "te37"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun 대소문자_공백_하이픈만_다른_새_부품은_같은_부품이고_검색과_추천에_나온다() {
        val name = "TE-37 " + UUID.randomUUID().toString().substring(0, 8)
        val first = resolve("RAYS", name)
        val second = resolve("rays", " " + name.lowercase().replace("-", "") + " ")
        assertThat(first).isEqualTo(second)

        val carModelId = avanteNId()
        repeat(2) {
            partUsageConsumer.onPartUsageChanged(
                objectMapper.writeValueAsString(PartUsageChangedMessage(carModelId, setOf(first), emptySet())),
            )
        }

        mockMvc.perform(
            get("/parts").header(GatewayHeaders.MEMBER_ID, "1").param("q", name.replace("-", "").uppercase()),
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].id").value(first.toInt()))
            .andExpect(jsonPath("$.result.items[0].brandName").value("RAYS"))
            .andExpect(jsonPath("$.result.items[0].useCount").value(2))
        mockMvc.perform(get("/parts/suggest").header(GatewayHeaders.MEMBER_ID, "1").param("q", "rays $name"))
            .andExpect(jsonPath("$.result.items[0].id").value(first.toInt()))
            .andExpect(jsonPath("$.result.items[0].score").value(1.0))
        mockMvc.perform(get("/internal/parts").param("ids", first.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].name").value(name))
            .andExpect(jsonPath("$.result[0].category").value("CHASSIS"))
    }

    @Test
    fun 이름_없이_새_부품을_등록할_수_없다() {
        mockMvc.perform(
            post("/internal/parts/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"brandName\":\"RAYS\",\"partName\":\" \",\"category\":\"CHASSIS\"}"),
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PART"))
    }

    @Test
    fun 트림으로_차종_정보를_주고_없는_트림은_INVALID_TRIM_이다() {
        val trimId = avanteNTrimId()

        mockMvc.perform(get("/internal/car-trims/$trimId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.brand").value("현대"))
            .andExpect(jsonPath("$.result.modelName").value("아반떼 N"))
            .andExpect(jsonPath("$.result.generationCode").value("CN7"))
            .andExpect(jsonPath("$.result.startYear").value(2021))
            .andExpect(jsonPath("$.result.trimName").value("2.0T"))
        mockMvc.perform(get("/internal/car-trims").param("ids", "$trimId,999999"))
            .andExpect(jsonPath("$.result.length()").value(1))
        mockMvc.perform(get("/internal/car-trims/999999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_TRIM"))
        mockMvc.perform(get("/internal/car-models").param("ids", "${avanteNId()},999999"))
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].name").value("아반떼 N"))
            .andExpect(jsonPath("$.result[0].hidden").value(false))
    }

    // 게시물 서비스가 부르는 부품 등록. partId 를 돌려준다
    private fun resolve(brandName: String, partName: String): Long {
        val json = mockMvc.perform(
            post("/internal/parts/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"brandName\":\"$brandName\",\"partName\":\"$partName\",\"category\":\"CHASSIS\"}"),
        )
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.id").toLong()
    }

    private fun avanteNId(): Long {
        val json = mockMvc.perform(get("/car-models")).andReturn().response.contentAsString
        val ids: List<Number> = JsonPath.read(json, "$.result.items[?(@.name == '아반떼 N')].id")
        return ids.first().toLong()
    }

    private fun avanteNTrimId(): Long {
        val json = mockMvc.perform(get("/car-models/${avanteNId()}")).andReturn().response.contentAsString
        return JsonPath.read<Number>(json, "$.result.generations[0].trims[0].id").toLong()
    }
}

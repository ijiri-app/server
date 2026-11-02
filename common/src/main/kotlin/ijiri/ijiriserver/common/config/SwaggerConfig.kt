package ijiri.ijiriserver.common.config

import ijiri.ijiriserver.common.security.GatewayHeaders
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 서비스마다 자기 API 문서를 낸다 (로컬·테스트 전용, 운영은 springdoc 을 끈다). 서비스를 직접 부를 때는
 * 게이트웨이 대신 X-Member-Id 헤더를 넣어 로그인한 회원으로 호출한다.
 */
@Configuration
class SwaggerConfig(
    @Value("\${spring.application.name}") private val applicationName: String,
) {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Ijiri $applicationName API")
                    .description("Ijiri $applicationName API 문서. 운영에서는 nginx 게이트웨이가 토큰을 검증해 X-Member-Id 를 넣는다")
                    .version("1.0.0")
            )
            // 상대 경로라 로컬/배포 환경 어디서 열어도 현재 호스트로 요청
            .addServersItem(Server().url("/"))
            .addSecurityItem(SecurityRequirement().addList(MEMBER_ID_SCHEME_NAME))
            .components(
                Components()
                    .addSecuritySchemes(
                        MEMBER_ID_SCHEME_NAME,
                        SecurityScheme()
                            .name(GatewayHeaders.MEMBER_ID)
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER),
                    )
            )

    companion object {
        private const val MEMBER_ID_SCHEME_NAME = "MemberId"
    }
}

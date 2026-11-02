package ijiri.ijiriserver.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Ijiri Server API")
                    .description("Ijiri Server API 문서")
                    .version("1.0.0")
            )
            // 상대 경로라 로컬/배포 환경 어디서 열어도 현재 호스트로 요청
            .addServersItem(Server().url("/"))
            .addSecurityItem(SecurityRequirement().addList(JWT_SCHEME_NAME))
            .components(
                Components()
                    .addSecuritySchemes(
                        JWT_SCHEME_NAME,
                        SecurityScheme()
                            .name(JWT_SCHEME_NAME)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    )
            )

    companion object {
        private const val JWT_SCHEME_NAME = "JWT"
    }
}

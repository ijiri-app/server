package ijiri.ijiriserver.media.config

import ijiri.ijiriserver.common.security.ServiceSecurityConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val serviceSecurityConfig: ServiceSecurityConfig,
) {

    // DB 저장소(로컬·테스트)일 때 사진 조회는 공개이고, 업로드는 URL 의 서명(만료 시각 포함)으로 인증한다
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        serviceSecurityConfig.build(http) { a ->
            a.requestMatchers(HttpMethod.GET, "/images/**").permitAll()
            a.requestMatchers(HttpMethod.PUT, "/uploads/files/**").permitAll()
        }
}

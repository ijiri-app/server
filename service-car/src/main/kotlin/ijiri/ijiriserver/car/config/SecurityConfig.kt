package ijiri.ijiriserver.car.config

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

    // 차종 마스터는 로그인 없이 관심 차종을 고르는 온보딩에서 쓴다
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        serviceSecurityConfig.build(http) { a ->
            a.requestMatchers(HttpMethod.GET, "/car-models", "/car-models/*").permitAll()
        }
}

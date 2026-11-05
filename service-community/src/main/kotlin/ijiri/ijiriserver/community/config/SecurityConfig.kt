package ijiri.ijiriserver.community.config

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

    // 피드, 게시물 상세, 회원의 게시물 목록은 로그인 없이 본다 (로그인했으면 차단 관계를 반영한다)
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        serviceSecurityConfig.build(http) { a ->
            a.requestMatchers(HttpMethod.GET, "/feed", "/posts/*", "/members/*/posts").permitAll()
        }
}

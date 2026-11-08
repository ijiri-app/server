package ijiri.ijiriserver.user.config

import ijiri.ijiriserver.common.security.ServiceSecurityConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val serviceSecurityConfig: ServiceSecurityConfig,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // 가입·로그인·갱신·로그아웃은 토큰 없이 부른다. 로그아웃은 게이트웨이가 넘긴 회원 id 가 있으면 그 회원의 세션을 지운다
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        serviceSecurityConfig.build(http) { a ->
            a.requestMatchers("/auth/**").permitAll()
        }
}

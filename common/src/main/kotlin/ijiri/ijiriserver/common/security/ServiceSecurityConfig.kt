package ijiri.ijiriserver.common.security

import ijiri.ijiriserver.common.exception.handler.CustomAccessDeniedHandler
import ijiri.ijiriserver.common.exception.handler.CustomAuthenticationEntryPoint
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

typealias RequestMatcherRegistry =
    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry

/**
 * 모든 서비스가 같은 보안 설정을 쓴다. 각 서비스의 SecurityConfig 는 build 에 자기 공개 경로만 넘긴다.
 * `/internal` 아래는 내부망에서 다른 서비스만 부르고(nginx 는 404), `/admin` 아래는 게이트웨이가 넘긴 역할(DB 기준)로 막는다.
 */
@Configuration
class ServiceSecurityConfig(
    private val headerAuthenticationFilter: HeaderAuthenticationFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>,
) {

    fun build(http: HttpSecurity, permit: (RequestMatcherRegistry) -> Unit): SecurityFilterChain =
        http
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            // 익명 사용자("anonymousUser")를 principal 로 넣지 않아, 인증이 없으면 @AuthenticationPrincipal 이 null 이 된다
            .anonymous { it.disable() }
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { a ->
                a.requestMatchers("/internal/**", "/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                permit(a)
                a.requestMatchers("/admin/**").hasRole("ADMIN")
                a.anyRequest().authenticated()
            }
            .exceptionHandling {
                it
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }
            .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    // @Component 필터는 Spring Boot 가 서블릿 필터로도 자동 등록하므로, Security 체인에서만 돌도록 끈다
    @Bean
    fun headerAuthenticationFilterRegistration(
        filter: HeaderAuthenticationFilter,
    ): FilterRegistrationBean<HeaderAuthenticationFilter> {
        val registration = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = allowedOrigins
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}

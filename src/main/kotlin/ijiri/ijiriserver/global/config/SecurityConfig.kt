package ijiri.ijiriserver.global.config

import ijiri.ijiriserver.global.handler.CustomAccessDeniedHandler
import ijiri.ijiriserver.global.handler.CustomAuthenticationEntryPoint
import ijiri.ijiriserver.global.jwt.JwtAuthenticationFilter
import ijiri.ijiriserver.global.security.AdminVerifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val adminVerifier: AdminVerifier,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            // 익명 사용자("anonymousUser")를 principal 로 넣지 않아, 인증이 없으면 @AuthenticationPrincipal 이 null 이 된다
            .anonymous { it.disable() }
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { a ->
                a
                    .requestMatchers(
                        HttpMethod.GET,
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/images/**",
                        "/car-models",
                        "/car-models/*",
                        "/feed",
                        "/posts/*",
                        "/members/*/posts",
                    ).permitAll()
                    // 업로드 URL 의 서명(만료 시각 포함)으로 인증한다. presigned URL 과 같은 방식
                    .requestMatchers(HttpMethod.PUT, "/uploads/files/**").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers(
                        "/error",
                    ).permitAll()
                    // 역할을 바꾸면 다시 로그인하지 않아도 바로 반영되도록 요청마다 DB 의 역할을 확인한다
                    .requestMatchers("/admin/**").access { authentication, _ ->
                        val current = authentication.get()
                        AuthorizationDecision(
                            current != null
                                && current.isAuthenticated
                                && adminVerifier.isAdmin(current.name)
                        )
                    }
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    // @Component 필터는 Spring Boot 가 서블릿 필터로도 자동 등록하므로, Security 체인에서만 돌도록 끈다
    @Bean
    fun jwtAuthenticationFilterRegistration(
        filter: JwtAuthenticationFilter,
    ): FilterRegistrationBean<JwtAuthenticationFilter> {
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

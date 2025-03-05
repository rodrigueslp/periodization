package com.extrabox.periodization.config

import com.extrabox.periodization.security.CustomOAuth2UserService
import com.extrabox.periodization.security.JwtAuthenticationFilter
import com.extrabox.periodization.security.OAuth2LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2LoginSuccessHandler)
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
                )
            }
            .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOrigin("https://app.planilize.com.br")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "https://periodization-frontend-production.up.railway.app",
            "https://periodization-production.up.railway.app",
            "https://app.planilize.com.br"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.allowCredentials = true
        // Adicione o tempo máximo de cache para respostas preflight
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

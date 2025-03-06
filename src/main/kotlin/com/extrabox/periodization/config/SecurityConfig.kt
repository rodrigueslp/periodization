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
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
            // Importante: a configuração de CORS vem primeiro
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    // Permitir acesso público para os endpoints de autenticação e OPTIONS
                    .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**", "/health").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // É crucial permitir requests OPTIONS para CORS preflight
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
            // Adicione o filtro JWT antes do filtro de autenticação
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Não use applyPermitDefaultValues junto com allowedOriginPatterns
        // configuration.applyPermitDefaultValues()

        // Configuração de origens permitidas
        configuration.allowedOriginPatterns = listOf("*") // Em produção, use domínios específicos

        // Métodos HTTP permitidos
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")

        // Cabeçalhos permitidos
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin")

        // Cabeçalhos expostos
        configuration.exposedHeaders = listOf("Authorization")

        // Permitir credenciais (cookies, autenticação)
        configuration.allowCredentials = true

        // Tempo de cache para respostas preflight
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    // Adicione esta configuração WebMvcConfigurer para reforçar o CORS
    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns("*") // Em produção, use domínios específicos
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin")
                    .exposedHeaders("Authorization")
                    .allowCredentials(true)
                    .maxAge(3600)
            }
        }
    }

    // Adicione um endpoint de health check para verificar se a aplicação está rodando
    @Bean
    fun healthEndpoint(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/health").allowedOriginPatterns("*")
            }
        }
    }
}
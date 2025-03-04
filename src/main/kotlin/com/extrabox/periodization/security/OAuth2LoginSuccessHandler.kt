package com.extrabox.periodization.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException

@Component
class OAuth2LoginSuccessHandler(
    private val jwtUtils: JwtUtils,
    private val userDetailsServiceImpl: UserDetailsServiceImpl
) : SimpleUrlAuthenticationSuccessHandler() {

    @Value("\${app.oauth2.redirectUri}")
    private lateinit var redirectUri: String

    @Throws(IOException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val email = oAuth2User.attributes["email"] as String

        // Carregando UserDetails para gerar o token JWT
        val userDetails = userDetailsServiceImpl.loadUserByUsername(email)
        val token = jwtUtils.generateJwtToken(userDetails)

        // Construindo a URL de redirecionamento com o token
        val targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("token", token)
            .build().toUriString()

        clearAuthenticationAttributes(request)
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}

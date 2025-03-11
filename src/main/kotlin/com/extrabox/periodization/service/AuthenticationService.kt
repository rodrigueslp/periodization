package com.extrabox.periodization.service

import com.extrabox.periodization.model.auth.JwtResponse
import com.extrabox.periodization.model.auth.TokenRefreshResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationService(
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService
) {
    /**
     * Método central para autenticação via Google
     */
    @Transactional
    fun authenticateWithGoogle(googleToken: String): JwtResponse {
        // Obter informações do usuário
        val (user, userDetails) = userService.processGoogleToken(googleToken)

        // Gerar tokens JWT
        val accessToken = userService.generateAccessToken(userDetails)

        // Gerar refresh token
        val refreshToken = refreshTokenService.createRefreshToken(user.email, userDetails)

        // Construir e retornar a resposta
        return userService.buildJwtResponse(user, accessToken, refreshToken.token)
    }

    /**
     * Método para renovar o token de acesso
     */
    fun refreshToken(refreshToken: String): TokenRefreshResponse {
        // Verificar e obter o refresh token
        val tokenEntity = refreshTokenService.findAndValidateToken(refreshToken)

        // Obter detalhes do usuário
        val userDetails = userService.loadUserDetailsByEmail(tokenEntity.userEmail)

        // Gerar novo token de acesso
        val newAccessToken = userService.generateAccessToken(userDetails)

        // Retornar resposta
        return TokenRefreshResponse(
            accessToken = newAccessToken,
            refreshToken = tokenEntity.token
        )
    }
}

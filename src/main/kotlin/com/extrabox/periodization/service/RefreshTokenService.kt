package com.extrabox.periodization.service

import com.extrabox.periodization.entity.RefreshToken
import com.extrabox.periodization.exceptions.TokenRefreshException
import com.extrabox.periodization.repository.RefreshTokenRepository
import com.extrabox.periodization.security.JwtUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtUtils: JwtUtils
    // Removida dependência de UserDetailsProvider
) {
    @Value("\${app.jwt.refresh-expiration:604800000}")
    private var refreshTokenDurationMs: Long = 0

    fun createRefreshToken(userEmail: String, userDetails: UserDetails): RefreshToken {
        val token = jwtUtils.generateRefreshToken(userDetails)

        val refreshToken = RefreshToken(
            token = token,
            userEmail = userEmail,
            expiryDate = Instant.now().plusMillis(refreshTokenDurationMs)
        )

        return refreshTokenRepository.save(refreshToken)
    }

    fun findByToken(token: String): Optional<RefreshToken> {
        return refreshTokenRepository.findByToken(token)
    }

    fun verifyExpiration(token: RefreshToken): RefreshToken {
        if (token.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(token)
            throw TokenRefreshException(token.token, "Refresh token expirado. Por favor, faça login novamente")
        }

        return token
    }

    // Método combinado para encontrar e validar o token
    fun findAndValidateToken(token: String): RefreshToken {
        val refreshToken = findByToken(token)
            .orElseThrow { TokenRefreshException(token, "Refresh token não encontrado na base de dados!") }

        return verifyExpiration(refreshToken)
    }

    @Transactional
    fun deleteByUserEmail(userEmail: String) {
        refreshTokenRepository.deleteByUserEmail(userEmail)
    }
}

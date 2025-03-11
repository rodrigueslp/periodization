package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun findByUserEmail(userEmail: String): List<RefreshToken>
    fun deleteByUserEmail(userEmail: String)
}
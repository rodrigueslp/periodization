package com.extrabox.periodization.repository

import com.extrabox.periodization.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByGoogleId(googleId: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun existsByGoogleId(googleId: String): Boolean
}

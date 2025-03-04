package com.extrabox.periodization.service

import com.extrabox.periodization.entity.Role
import com.extrabox.periodization.entity.User
import com.extrabox.periodization.model.auth.JwtResponse
import com.extrabox.periodization.model.auth.UserInfoResponse
import com.extrabox.periodization.repository.RoleRepository
import com.extrabox.periodization.repository.UserRepository
import com.extrabox.periodization.security.GoogleTokenVerifier
import com.extrabox.periodization.security.GoogleUserInfo
import com.extrabox.periodization.security.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val jwtUtils: JwtUtils,
    private val googleTokenVerifier: GoogleTokenVerifier
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun validateToken(googleToken: String): JwtResponse {
        // Verifica o token do Google
        val googleUserInfo = googleTokenVerifier.verifyWithGoogle(googleToken)
            ?: throw RuntimeException("Token inválido ou expirado")

        logger.info("Token do Google validado para o usuário: ${googleUserInfo.email}")

        // Verificando se o usuário já existe ou criando um novo
        val user = userRepository.findByEmail(googleUserInfo.email).orElseGet {
            // Cria novo usuário
            val newUser = User(
                email = googleUserInfo.email,
                fullName = googleUserInfo.name,
                googleId = googleUserInfo.sub,
                profilePicture = googleUserInfo.picture,
                active = true
            )

            // Atribui papel USER por padrão
            val userRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseGet {
                    val newRole = Role(name = Role.ROLE_USER)
                    roleRepository.save(newRole)
                }

            newUser.roles.add(userRole)
            userRepository.save(newUser)
        }

        // Atualiza dados do usuário, se necessário
        updateUserInfo(user, googleUserInfo)

        // Cria um UserDetails para gerar o token JWT
        val authorities = user.roles.map { SimpleGrantedAuthority(it.name) }
        val userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.email)
            .password("")
            .authorities(authorities)
            .build()

        // Gera token JWT
        val token = jwtUtils.generateJwtToken(userDetails)

        val roles = user.roles.map { it.name }
        val hasActiveSubscription = user.subscriptionExpiry?.isAfter(LocalDateTime.now()) ?: false

        return JwtResponse(
            token = token,
            id = user.id ?: 0,
            email = user.email,
            name = user.fullName,
            roles = roles,
            profilePicture = user.profilePicture,
            subscriptionPlan = user.subscriptionPlan,
            hasActiveSubscription = hasActiveSubscription
        )
    }

    private fun updateUserInfo(user: User, googleUserInfo: GoogleUserInfo) {
        var updated = false

        if (user.googleId != googleUserInfo.sub) {
            user.googleId = googleUserInfo.sub
            updated = true
        }

        if (user.fullName != googleUserInfo.name) {
            user.fullName = googleUserInfo.name
            updated = true
        }

        if (user.profilePicture != googleUserInfo.picture) {
            user.profilePicture = googleUserInfo.picture
            updated = true
        }

        // Atualiza o último login
        user.lastLogin = LocalDateTime.now()
        updated = true

        if (updated) {
            userRepository.save(user)
        }
    }

    fun getUserInfo(email: String): UserInfoResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $email") }

        val roles = user.roles.map { it.name }
        val hasActiveSubscription = user.subscriptionExpiry?.isAfter(LocalDateTime.now()) ?: false

        return UserInfoResponse(
            id = user.id ?: 0,
            email = user.email,
            fullName = user.fullName,
            profilePicture = user.profilePicture,
            createdAt = user.createdAt,
            lastLogin = user.lastLogin,
            roles = roles,
            subscriptionPlan = user.subscriptionPlan,
            subscriptionExpiry = user.subscriptionExpiry,
            hasActiveSubscription = hasActiveSubscription
        )
    }

    fun checkSubscription(email: String): Map<String, Any> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $email") }

        val hasActiveSubscription = user.subscriptionExpiry?.isAfter(LocalDateTime.now()) ?: false

        return mapOf(
            "hasActiveSubscription" to hasActiveSubscription,
            "subscriptionPlan" to (user.subscriptionPlan ?: "none"),
            "subscriptionExpiry" to (user.subscriptionExpiry?.toString() ?: "")
        )
    }

    @Transactional
    fun updateSubscription(email: String, plan: String, months: Int) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $email") }

        user.subscriptionPlan = plan
        user.subscriptionExpiry = LocalDateTime.now().plusMonths(months.toLong())

        // Adicionar ROLE_PREMIUM se não existir
        val premiumRole = roleRepository.findByName(Role.ROLE_PREMIUM)
            .orElseGet {
                val newRole = Role(name = Role.ROLE_PREMIUM)
                roleRepository.save(newRole)
            }

        if (!user.roles.any { it.name == Role.ROLE_PREMIUM }) {
            user.roles.add(premiumRole)
        }

        userRepository.save(user)
    }
}

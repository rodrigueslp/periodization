package com.extrabox.periodization.security

import com.extrabox.periodization.entity.Role
import com.extrabox.periodization.entity.User
import com.extrabox.periodization.repository.RoleRepository
import com.extrabox.periodization.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : DefaultOAuth2UserService() {

    private val logger = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.attributes

        // Extraindo informações do usuário Google
        val email = attributes["email"] as String
        val name = attributes["name"] as String
        val googleId = attributes["sub"] as String
        val pictureUrl = attributes["picture"] as? String

        // Verificando se o usuário já existe ou criando um novo
        val userOptional = userRepository.findByEmail(email)

        if (userOptional.isPresent) {
            val user = userOptional.get()
            updateExistingUser(user, googleId, name, pictureUrl)
        } else {
            createNewUser(email, googleId, name, pictureUrl)
        }

        return oAuth2User
    }

    private fun updateExistingUser(user: User, googleId: String, name: String, pictureUrl: String?) {
        user.googleId = googleId
        user.fullName = name
        user.profilePicture = pictureUrl
        user.lastLogin = LocalDateTime.now()
        userRepository.save(user)
        logger.info("Usuário existente atualizado: ${user.email}")
    }

    private fun createNewUser(email: String, googleId: String, name: String, pictureUrl: String?) {
        val user = User(
            email = email,
            fullName = name,
            googleId = googleId,
            profilePicture = pictureUrl,
            active = true
        )

        // Atribuindo papel USER por padrão
        val userRole = roleRepository.findByName(Role.ROLE_USER)
            .orElseGet {
                val newRole = Role(name = Role.ROLE_USER)
                roleRepository.save(newRole)
            }

        user.roles.add(userRole)
        userRepository.save(user)
        logger.info("Novo usuário criado: $email")
    }
}

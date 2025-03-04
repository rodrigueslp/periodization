package com.extrabox.periodization.security

import com.extrabox.periodization.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $username") }

        val authorities = user.roles.map { role ->
            SimpleGrantedAuthority(role.name)
        }

        return User.builder()
            .username(user.email)
            .password("") // Não precisamos de senha com autenticação OAuth2
            .authorities(authorities)
            .accountExpired(!user.active)
            .accountLocked(!user.active)
            .credentialsExpired(false)
            .disabled(!user.active)
            .build()
    }
}

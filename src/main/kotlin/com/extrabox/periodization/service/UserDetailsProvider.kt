package com.extrabox.periodization.service


interface UserDetailsProvider {
    fun loadUserDetailsByEmail(email: String): org.springframework.security.core.userdetails.UserDetails
}

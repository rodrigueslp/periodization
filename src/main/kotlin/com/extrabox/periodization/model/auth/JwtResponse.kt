package com.extrabox.periodization.model.auth

data class JwtResponse(
    val token: String,
    val type: String = "Bearer",
    val id: Long,
    val email: String,
    val name: String,
    val roles: List<String>,
    val profilePicture: String? = null,
    val subscriptionPlan: String? = null,
    val hasActiveSubscription: Boolean = false
)

package com.extrabox.periodization.model.auth

import java.time.LocalDateTime

data class UserInfoResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val profilePicture: String?,
    val createdAt: LocalDateTime,
    val lastLogin: LocalDateTime?,
    val roles: List<String>,
    val subscriptionPlan: String?,
    val subscriptionExpiry: LocalDateTime?,
    val hasActiveSubscription: Boolean
)

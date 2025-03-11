package com.extrabox.periodization.model

data class FeedbackResponse(
    val id: Long,
    val feedbackText: String,
    val feedbackType: String,
    val createdAt: String,
    val userName: String,
    val userEmail: String,
    val planId: String? = null
)

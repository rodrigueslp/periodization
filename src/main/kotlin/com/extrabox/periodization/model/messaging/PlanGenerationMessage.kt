package com.extrabox.periodization.model.messaging

data class PlanGenerationMessage(
    val planId: String,
    val userEmail: String
)

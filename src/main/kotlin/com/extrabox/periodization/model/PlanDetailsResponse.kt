package com.extrabox.periodization.model

data class PlanDetailsResponse(
    val planId: String,
    val athleteName: String,
    val athleteAge: Int,
    val athleteWeight: Double,
    val athleteHeight: Int,
    val experienceLevel: String,
    val trainingGoal: String,
    val availability: Int,
    val injuries: String?,
    val trainingHistory: String?,
    val planDuration: Int,
    val planContent: String,
    val createdAt: String,
    val benchmarks: Map<String, Any?>?
)

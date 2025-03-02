package com.extrabox.periodization.model

data class PlanRequest(
    val athleteData: AthleteData,
    val planDuration: Int = 12 // Duração padrão em semanas
)
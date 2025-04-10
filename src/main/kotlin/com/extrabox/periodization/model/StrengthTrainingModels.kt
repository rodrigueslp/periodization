package com.extrabox.periodization.model

// Modelo para dados do atleta para treino de musculação
data class StrengthAthleteData(
    val nome: String,
    val idade: Int,
    val peso: Double,
    val altura: Int,
    val experiencia: String,
    val objetivo: String,
    val disponibilidade: Int,
    val lesoes: String? = null,
    val historico: String? = null,
    val objetivoDetalhado: String? = null,
    val periodoTreino: String? = null,
    val trainingFocus: String,
    val equipmentAvailable: String? = null,
    val sessionsPerWeek: Int,
    val sessionDuration: Int
)

// Modelo para requisição de plano de musculação
data class StrengthPlanRequest(
    val athleteData: StrengthAthleteData,
    val planDuration: Int,
    val startDate: String? = null
)

// Modelo para resposta de plano de musculação
data class StrengthPlanResponse(
    val planId: String,
    val message: String
)

// Modelo para detalhes do plano de musculação
data class StrengthPlanDetailsResponse(
    val planId: String,
    val athleteName: String,
    val athleteAge: Int,
    val athleteWeight: Double,
    val athleteHeight: Int,
    val experienceLevel: String,
    val trainingGoal: String,
    val availability: Int,
    val injuries: String? = null,
    val trainingHistory: String? = null,
    val planDuration: Int,
    val planContent: String,
    val trainingFocus: String,
    val equipmentAvailable: String? = null,
    val sessionsPerWeek: Int,
    val sessionDuration: Int,
    val pdfFilePath: String,
    val excelFilePath: String,
    val createdAt: String,
    val status: com.extrabox.periodization.enums.PlanStatus,
    val canGenerate: Boolean,
    val startDate: String? = null,
    val endDate: String? = null
)
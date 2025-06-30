package com.extrabox.periodization.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// Modelo para dados do atleta para treino de corrida
data class RunningAthleteData(
    @field:NotBlank(message = "Nome é obrigatório")
    val nome: String,

    @field:NotNull(message = "Idade é obrigatória")
    @field:Min(14, message = "Idade mínima é 14 anos")
    @field:Max(80, message = "Idade máxima é 80 anos")
    val idade: Int,

    @field:NotNull(message = "Peso é obrigatório")
    @field:Min(30, message = "Peso mínimo é 30 kg")
    @field:Max(200, message = "Peso máximo é 200 kg")
    val peso: Double,

    @field:NotNull(message = "Altura é obrigatória")
    @field:Min(100, message = "Altura mínima é 100 cm")
    @field:Max(220, message = "Altura máxima é 220 cm")
    val altura: Int,

    @field:NotBlank(message = "Nível de experiência é obrigatório")
    val experiencia: String, // iniciante, intermediário, avançado

    @field:NotBlank(message = "Objetivo é obrigatório")
    val objetivo: String, // 5k, 10k, 21k, 42k, condicionamento geral

    @field:NotNull(message = "Dias disponíveis por semana é obrigatório")
    @field:Min(3, message = "Mínimo 3 dias por semana")
    @field:Max(7, message = "Máximo 7 dias por semana")
    val diasDisponiveis: Int,

    @field:NotNull(message = "Volume semanal atual é obrigatório")
    @field:Min(0, message = "Volume mínimo é 0 km")
    @field:Max(200, message = "Volume máximo é 200 km")
    val volumeSemanalAtual: Int, // km por semana atual

    val paceAtual5k: String? = null, // "05:30/km" ou "05:30"
    val paceAtual10k: String? = null,
    val melhorTempo5k: String? = null, // "25:30"
    val melhorTempo10k: String? = null,
    val melhorTempo21k: String? = null,
    val melhorTempo42k: String? = null,

    val tempoObjetivo: String? = null, // "sub 20min nos 5k"
    val dataProva: String? = null, // quando é a prova alvo

    val historicoLesoes: String? = null,
    val experienciaAnterior: String? = null,
    val preferenciaTreino: String? = null, // manhã, tarde, noite
    val localTreino: String? = null, // rua, esteira, pista, trilha
    val equipamentosDisponiveis: String? = null // GPS, monitor cardíaco, etc
)

// Modelo para requisição de plano de corrida
data class RunningPlanRequest(
    val athleteData: RunningAthleteData,
    val planDuration: Int, // duração em semanas
    val startDate: String? = null
)

// Modelo para resposta de plano de corrida
data class RunningPlanResponse(
    val planId: String,
    val message: String
)

// Modelo para detalhes do plano de corrida
data class RunningPlanDetailsResponse(
    val planId: String,
    val athleteName: String,
    val athleteAge: Int,
    val athleteWeight: Double,
    val athleteHeight: Int,
    val experienceLevel: String,
    val trainingGoal: String,
    val diasDisponiveis: Int,
    val volumeSemanalAtual: Int,
    val paceAtual5k: String?,
    val paceAtual10k: String?,
    val melhorTempo5k: String?,
    val melhorTempo10k: String?,
    val melhorTempo21k: String?,
    val melhorTempo42k: String?,
    val tempoObjetivo: String?,
    val dataProva: String?,
    val historicoLesoes: String?,
    val experienciaAnterior: String?,
    val preferenciaTreino: String?,
    val localTreino: String?,
    val equipamentosDisponiveis: String?,
    val planDuration: Int,
    val planContent: String,
    val pdfFilePath: String,
    val excelFilePath: String,
    val createdAt: String,
    val status: com.extrabox.periodization.enums.PlanStatus,
    val canGenerate: Boolean,
    val startDate: String? = null,
    val endDate: String? = null
)

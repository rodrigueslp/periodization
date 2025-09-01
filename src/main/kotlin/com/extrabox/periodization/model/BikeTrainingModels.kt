package com.extrabox.periodization.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// Modelo para dados do atleta para treino de bike
data class BikeAthleteData(
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
    val objetivo: String, // condicionamento, speed, mountain bike, triathlon, competição

    @field:NotNull(message = "Dias disponíveis por semana é obrigatório")
    @field:Min(3, message = "Mínimo 3 dias por semana")
    @field:Max(7, message = "Máximo 7 dias por semana")
    val diasDisponiveis: Int,

    @field:NotNull(message = "Volume semanal atual é obrigatório")
    @field:Min(0, message = "Volume mínimo é 0 horas")
    @field:Max(30, message = "Volume máximo é 30 horas")
    val volumeSemanalAtual: Int, // horas por semana atual

    val tipoBike: String? = null, // speed, mountain bike, indoor, gravel
    val ftpAtual: Int? = null, // Functional Threshold Power em watts
    val potenciaMediaAtual: Int? = null, // watts médios
    val melhorTempo40km: String? = null, // "01:15:30"
    val melhorTempo100km: String? = null,
    val melhorTempo160km: String? = null,

    val tempoObjetivo: String? = null, // "sub 1h nos 40km"
    val dataProva: String? = null, // quando é a prova alvo

    val historicoLesoes: String? = null,
    val experienciaAnterior: String? = null,
    val preferenciaTreino: String? = null, // indoor, outdoor, misto
    val equipamentosDisponiveis: String? = null, // smart trainer, power meter, monitor cardíaco
    val zonaTreinoPreferida: String? = null // resistência, força, velocidade, HIIT
)

// Modelo para requisição de plano de bike
data class BikePlanRequest(
    val athleteData: BikeAthleteData,
    val planDuration: Int, // duração em semanas
    val startDate: String? = null
)

// Modelo para resposta de plano de bike
data class BikePlanResponse(
    val planId: String,
    val message: String
)

// Modelo para detalhes do plano de bike
data class BikePlanDetailsResponse(
    val planId: String,
    val athleteName: String,
    val athleteAge: Int,
    val athleteWeight: Double,
    val athleteHeight: Int,
    val experienceLevel: String,
    val trainingGoal: String,
    val diasDisponiveis: Int,
    val volumeSemanalAtual: Int,
    val tipoBike: String?,
    val ftpAtual: Int?,
    val potenciaMediaAtual: Int?,
    val melhorTempo40km: String?,
    val melhorTempo100km: String?,
    val melhorTempo160km: String?,
    val tempoObjetivo: String?,
    val dataProva: String?,
    val historicoLesoes: String?,
    val experienciaAnterior: String?,
    val preferenciaTreino: String?,
    val equipamentosDisponiveis: String?,
    val zonaTreinoPreferida: String?,
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

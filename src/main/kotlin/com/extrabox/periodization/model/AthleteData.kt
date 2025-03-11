package com.extrabox.periodization.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AthleteData(
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
    val experiencia: String,

    @field:NotBlank(message = "Objetivo é obrigatório")
    val objetivo: String,

    @field:NotNull(message = "Disponibilidade semanal é obrigatória")
    @field:Min(3, message = "Disponibilidade mínima é 3 dias")
    @field:Max(7, message = "Disponibilidade máxima é 7 dias")
    val disponibilidade: Int,

    val objetivoDetalhado: String? = null,
    val lesoes: String? = null,
    val historico: String? = null,
    val treinoPrincipal: Boolean = false,
    val periodoTreino: String? = null,
    val benchmarks: Benchmarks? = null
)

data class Benchmarks(
    val backSquat: Double? = null,
    val deadlift: Double? = null,
    val clean: Double? = null,
    val snatch: Double? = null,
    val fran: String? = null,
    val grace: String? = null
)

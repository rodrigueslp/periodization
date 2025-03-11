package com.extrabox.periodization.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FeedbackRequest(
    @field:NotBlank(message = "O texto do feedback é obrigatório")
    @field:Size(min = 3, max = 500, message = "O feedback deve ter entre 3 e 500 caracteres")
    val feedbackText: String,

    @field:NotBlank(message = "O tipo de feedback é obrigatório")
    val feedbackType: String, // GENERAL, BUG, FEATURE_REQUEST, IMPROVEMENT

    val planId: String? = null // ID opcional de um plano relacionado
)

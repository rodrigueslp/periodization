package com.extrabox.periodization.model.messaging

import com.extrabox.periodization.enums.PlanType
import java.io.Serializable

data class PlanGenerationMessage(
    val planId: String,
    val userEmail: String,
    val planType: PlanType // Padrão para compatibilidade com código existente
) : Serializable

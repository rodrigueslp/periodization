package com.extrabox.periodization.model.payment

import java.time.LocalDateTime

data class PaymentResponse(
    val paymentId: String,
    val preferenceId: String,
    val externalReference: String,
    val status: String,
    val paymentUrl: String,
    val createdAt: String,
    // Campos para PIX
    val pixCopiaECola: String? = null,
    val qrCodeBase64: String? = null,
    // Campos adicionais para administrador
    val amount: Double? = null,
    val description: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
    val planId: String? = null,
    val updatedAt: String? = null
)
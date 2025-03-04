package com.extrabox.periodization.model.payment

// Modifique seu PaymentResponse.kt para incluir dados do PIX
data class PaymentResponse(
    val paymentId: String,
    val preferenceId: String,
    val externalReference: String,
    val status: String,
    val paymentUrl: String,
    val createdAt: String,
    // Campos para PIX
    val pixCopiaECola: String? = null,
    val qrCodeBase64: String? = null
)

package com.extrabox.periodization.model.payment

/**
 * Modelo de dados para requisição de pagamento
 */
data class PaymentRequest(
    /**
     * ID do plano de treinamento associado a este pagamento
     */
    val planId: String,

    /**
     * Descrição do produto/serviço sendo pago
     * Padrão: "Plano de Periodização CrossFit"
     */
    val description: String = "Plano de Periodização CrossFit",

    /**
     * Valor do pagamento
     * Padrão: 9.90 (R$)
     */
    val amount: Double = 9.90,

    /**
     * Método de pagamento preferido
     * Valores aceitos: "pix", "credit_card", null (assume pix como padrão)
     */
    val paymentMethod: String? = null,

    /**
     * Token do cartão (apenas para pagamentos transparentes com cartão)
     */
    val token: String? = null,

    /**
     * ID do método de pagamento (visa, mastercard, etc) para pagamentos com cartão
     */
    val paymentMethodId: String? = null,

    /**
     * Número de parcelas para pagamentos com cartão
     * Padrão: 1
     */
    val installments: Int = 1,

    /**
     * Email do pagador (opcional, será usado o email do usuário logado se não informado)
     */
    val email: String? = null
)
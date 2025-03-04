package com.extrabox.periodization.enums

enum class PlanStatus {
    PAYMENT_PENDING,   // Plano criado, aguardando pagamento
    PAYMENT_APPROVED,  // Pagamento aprovado, aguardando geração
    GENERATING,        // Em processo de geração
    COMPLETED,         // Plano completamente gerado
    FAILED             // Falha na geração
}
package com.extrabox.periodization.enums

enum class PlanStatus {
    PAYMENT_PENDING,   // Plano criado, aguardando pagamento
    PAYMENT_APPROVED,  // Pagamento aprovado, aguardando geração
    QUEUED,            // Enviado para a fila, aguardando processamento
    GENERATING,        // Em processo de geração
    COMPLETED,         // Plano completamente gerado
    FAILED             // Falha na geração
}
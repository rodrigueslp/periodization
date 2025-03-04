package com.extrabox.periodization.controller

import com.extrabox.periodization.model.payment.PaymentRequest
import com.extrabox.periodization.model.payment.PaymentResponse
import com.extrabox.periodization.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagamentos", description = "Endpoints para gestão de pagamentos")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping
    @Operation(summary = "Criar novo pagamento", description = "Cria um novo pagamento para um plano de periodização")
    @SecurityRequirement(name = "bearerAuth")
    fun createPayment(
        @RequestBody paymentRequest: PaymentRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PaymentResponse> {
        val response = paymentService.createPayment(paymentRequest, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @Operation(summary = "Listar pagamentos do usuário", description = "Retorna o histórico de pagamentos do usuário")
    @SecurityRequirement(name = "bearerAuth")
    fun getUserPayments(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<PaymentResponse>> {
        val payments = paymentService.getUserPayments(userDetails.username)
        return ResponseEntity.ok(payments)
    }

    @GetMapping("/status/{externalReference}")
    @Operation(summary = "Verificar status do pagamento", description = "Verifica o status de um pagamento pelo seu ID de referência externa")
    @SecurityRequirement(name = "bearerAuth")
    fun checkPaymentStatus(
        @PathVariable externalReference: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, String>> {
        val status = paymentService.checkPaymentStatus(externalReference, userDetails.username)
        return ResponseEntity.ok(mapOf("status" to status))
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook do Mercado Pago", description = "Endpoint para receber notificações do Mercado Pago")
    fun handleWebhook(@RequestBody data: Map<String, Any>): ResponseEntity<String> {
        val response = paymentService.processWebhook(data)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os pagamentos", description = "Retorna todos os pagamentos (apenas para administradores)")
    fun getAllPayments(): ResponseEntity<List<PaymentResponse>> {
        // Implementar no PaymentService um método para listar todos os pagamentos
        // Esta é apenas uma sugestão de endpoint para administrativo
        val payments = listOf<PaymentResponse>() // Substituir pela chamada real
        return ResponseEntity.ok(payments)
    }

    @PostMapping("/test/approve/{externalReference}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprovar pagamento (teste)", description = "Aprova manualmente um pagamento (apenas para testes)")
    fun approvePayment(
        @PathVariable externalReference: String
    ): ResponseEntity<Map<String, String>> {
        // Implementar no PaymentService um método para aprovar manualmente um pagamento
        // Esta é apenas uma sugestão para facilitar testes
        return ResponseEntity.ok(mapOf("message" to "Pagamento aprovado manualmente"))
    }

    @PostMapping("/simulate-approval/{externalReference}")
    @Operation(summary = "Simular aprovação de pagamento para testes",
        description = "Simula a aprovação de um pagamento")
    @SecurityRequirement(name = "bearerAuth")
    fun simulatePaymentApproval(
        @PathVariable externalReference: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, String>> {
        val status = paymentService.simulatePaymentApproval(externalReference, userDetails.username)
        return ResponseEntity.ok(mapOf("status" to status))
    }

}
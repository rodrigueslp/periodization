package com.extrabox.periodization.service

import com.extrabox.periodization.entity.Payment
import com.extrabox.periodization.model.payment.PaymentRequest
import com.extrabox.periodization.model.payment.PaymentResponse
import com.extrabox.periodization.repository.PaymentRepository
import com.extrabox.periodization.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class PaymentService(
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val userService: UserService,

    @Value("\${mercado-pago.access-token}")
    private val mercadoPagoAccessToken: String,

    @Value("\${app.frontend-url}")
    private val frontendUrl: String
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    private val client = OkHttpClient()
    private val objectMapper = ObjectMapper()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Transactional
    fun createPayment(request: PaymentRequest, userEmail: String): PaymentResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Referência externa única para este pagamento
        val externalReference = UUID.randomUUID().toString()

        try {
            // Verificar se o pagamento será por PIX
            val isPix = request.paymentMethod == "pix" || request.paymentMethod == null

            if (isPix) {
                // Configurar dados para pagamento PIX
                val paymentJson = """
                    {
                        "transaction_amount": ${request.amount},
                        "description": "${request.description}",
                        "payment_method_id": "pix",
                        "payer": {
                            "email": "${user.email}"
                        },
                        "external_reference": "$externalReference"
                    }
                """.trimIndent()

                // Criar a requisição para pagamento PIX no Mercado Pago
                val pixRequest = Request.Builder()
                    .url("https://api.mercadopago.com/v1/payments")
                    .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Idempotency-Key", UUID.randomUUID().toString())
                    .post(paymentJson.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(pixRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Sem detalhes"
                        logger.error("Erro na API do Mercado Pago (PIX): $errorBody")
                        throw RuntimeException("Erro ao criar pagamento PIX: ${response.code} - $errorBody")
                    }

                    // Ler a resposta
                    val responseBody = response.body?.string()
                        ?: throw RuntimeException("Corpo da resposta vazio")

                    val paymentResult = objectMapper.readValue<Map<String, Any>>(responseBody)
                    val paymentId = paymentResult["id"].toString()
                    val status = paymentResult["status"] as String

                    // Extrair dados do PIX
                    val pointOfInteraction = paymentResult["point_of_interaction"] as? Map<String, Any>
                    val transactionData = pointOfInteraction?.get("transaction_data") as? Map<String, Any>

                    val pixCopiaECola = transactionData?.get("qr_code") as? String
                    val qrCodeBase64 = transactionData?.get("qr_code_base64") as? String

                    // Persistir o pagamento no banco de dados
                    val payment = Payment(
                        paymentId = paymentId,
                        preferenceId = "", // PIX não usa preferenceId
                        externalReference = externalReference,
                        amount = request.amount,
                        status = status,
                        description = request.description,
                        user = user,
                        planId = request.planId
                    )

                    paymentRepository.save(payment)

                    return PaymentResponse(
                        paymentId = paymentId,
                        preferenceId = "",
                        externalReference = externalReference,
                        status = status,
                        paymentUrl = "",
                        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                        pixCopiaECola = pixCopiaECola,
                        qrCodeBase64 = qrCodeBase64
                    )
                }
            } else {
                // Configuração para Checkout Pro (cartão de crédito, etc)
                val preferenceJson = """
                    {
                        "items": [
                            {
                                "title": "${request.description}",
                                "quantity": 1,
                                "currency_id": "BRL",
                                "unit_price": ${request.amount}
                            }
                        ],
                        "external_reference": "$externalReference",
                        "back_urls": {
                            "success": "$frontendUrl/payment/success",
                            "pending": "$frontendUrl/payment/pending",
                            "failure": "$frontendUrl/payment/failure"
                        },
                        "auto_return": "approved",
                        "payment_methods": {
                            "excluded_payment_types": [
                                {"id": "ticket"}
                            ],
                            "installments": 1
                        },
                        "statement_descriptor": "Periodização CrossFit",
                        "notification_url": "$frontendUrl/api/payments/webhook"
                    }
                """.trimIndent()

                // Criar a requisição para o Checkout Pro no Mercado Pago
                val checkoutRequest = Request.Builder()
                    .url("https://api.mercadopago.com/checkout/preferences")
                    .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Idempotency-Key", UUID.randomUUID().toString())
                    .post(preferenceJson.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(checkoutRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Sem detalhes"
                        logger.error("Erro na API do Mercado Pago (Checkout): $errorBody")
                        throw RuntimeException("Erro ao criar preferência: ${response.code} - $errorBody")
                    }

                    // Ler a resposta
                    val responseBody = response.body?.string()
                        ?: throw RuntimeException("Corpo da resposta vazio")

                    val preference = objectMapper.readValue<Map<String, Any>>(responseBody)
                    val preferenceId = preference["id"] as String
                    val initPoint = preference["init_point"] as String

                    // Registrar o pagamento no banco
                    val payment = Payment(
                        paymentId = UUID.randomUUID().toString(), // ID temporário
                        preferenceId = preferenceId,
                        externalReference = externalReference,
                        amount = request.amount,
                        status = "PENDING",
                        description = request.description,
                        user = user,
                        planId = request.planId
                    )

                    paymentRepository.save(payment)

                    // Retornar resposta com URL de pagamento
                    return PaymentResponse(
                        paymentId = payment.paymentId,
                        preferenceId = preferenceId,
                        externalReference = externalReference,
                        status = "PENDING",
                        paymentUrl = initPoint,
                        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Erro ao criar pagamento: ${e.message}", e)
            throw RuntimeException("Erro ao processar pagamento: ${e.message}")
        }
    }

    @Transactional
    fun processWebhook(data: Map<String, Any>): String {
        logger.info("Webhook recebido: $data")

        try {
            // Extrair dados da notificação
            val type = data["type"] as? String ?: return "Tipo de notificação desconhecido"
            val dataInfo = data["data"] as? Map<String, Any> ?: return "Dados da notificação inválidos"

            if (type == "payment") {
                val paymentId = dataInfo["id"] as? String ?: return "ID de pagamento não encontrado"

                // Obter detalhes do pagamento
                val paymentRequest = Request.Builder()
                    .url("https://api.mercadopago.com/v1/payments/$paymentId")
                    .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                    .get()
                    .build()

                client.newCall(paymentRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        return "Erro ao consultar pagamento: ${response.code}"
                    }

                    val paymentData = objectMapper.readValue<Map<String, Any>>(
                        response.body?.string() ?: return "Resposta vazia"
                    )

                    val status = paymentData["status"] as String
                    val externalReference = paymentData["external_reference"] as? String
                        ?: return "Referência externa não encontrada"

                    // Buscar pagamento no banco de dados
                    val payment = externalReference.let { ref ->
                        paymentRepository.findByExternalReference(ref)
                            .orElse(null) ?: return "Pagamento não encontrado com referência: $ref"
                    }

                    // Se vier do Checkout Pro, atualizar paymentId
                    if (payment.paymentId.length < 10) {
                        payment.paymentId = paymentId
                    }

                    // Atualizar status
                    payment.status = status
                    payment.updatedAt = LocalDateTime.now()
                    paymentRepository.save(payment)

                    // Se aprovado, atualizar assinatura do usuário
                    if (status == "approved") {
                        val user = payment.user
                        if (user != null) {
                            userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                            logger.info("Assinatura ativada para usuário: ${user.email}")
                        }
                    }

                    return "Pagamento processado com sucesso: $status"
                }
            }

            return "Notificação recebida, mas não processada: $type"
        } catch (e: Exception) {
            logger.error("Erro ao processar webhook: ${e.message}", e)
            return "Erro ao processar webhook: ${e.message}"
        }
    }

    fun checkPaymentStatus(externalReference: String, userEmail: String): String {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val payment = paymentRepository.findByExternalReference(externalReference)
            .orElseThrow { RuntimeException("Pagamento não encontrado com referência: $externalReference") }

        // Verificar se o pagamento pertence ao usuário
        if (payment.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Este pagamento não pertence ao usuário logado")
        }

        // Se o pagamento já estiver aprovado, retornar status atual
        if (payment.status == "approved") {
            return payment.status
        }

        // Consultar status atual no Mercado Pago
        try {
            // Se for PIX, consultar diretamente
            if (payment.paymentId.length > 5) {
                val request = Request.Builder()
                    .url("https://api.mercadopago.com/v1/payments/${payment.paymentId}")
                    .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return payment.status
                    }

                    val responseBody = response.body?.string() ?: return payment.status
                    val paymentData = objectMapper.readValue<Map<String, Any>>(responseBody)
                    val currentStatus = paymentData["status"] as String

                    // Atualizar status no banco de dados se diferente
                    if (currentStatus != payment.status) {
                        payment.status = currentStatus
                        payment.updatedAt = LocalDateTime.now()
                        paymentRepository.save(payment)

                        // Se aprovado, atualizar assinatura do usuário
                        if (currentStatus == "approved") {
                            userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                        }
                    }

                    return currentStatus
                }
            } else if (payment.preferenceId.isNotEmpty()) {
                // Para Checkout Pro, consultar por preferência
                val request = Request.Builder()
                    .url("https://api.mercadopago.com/checkout/preferences/${payment.preferenceId}")
                    .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return payment.status
                    }

                    val responseBody = response.body?.string() ?: return payment.status

                    // As preferências não contêm o status de pagamento
                    // Precisamos consultar pagamentos associados a essa preferência
                    val searchRequest = Request.Builder()
                        .url("https://api.mercadopago.com/v1/payments/search?external_reference=${payment.externalReference}")
                        .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                        .get()
                        .build()

                    client.newCall(searchRequest).execute().use { searchResponse ->
                        if (!searchResponse.isSuccessful) {
                            return payment.status
                        }

                        val searchBody = searchResponse.body?.string() ?: return payment.status
                        val searchData = objectMapper.readValue<Map<String, Any>>(searchBody)
                        val results = searchData["results"] as? List<Map<String, Any>> ?: return payment.status

                        if (results.isNotEmpty()) {
                            val latestPayment = results[0]
                            val newPaymentId = latestPayment["id"].toString()
                            val currentStatus = latestPayment["status"] as String

                            // Atualizar payment_id e status
                            payment.paymentId = newPaymentId
                            payment.status = currentStatus
                            payment.updatedAt = LocalDateTime.now()
                            paymentRepository.save(payment)

                            // Se aprovado, atualizar assinatura do usuário
                            if (currentStatus == "approved") {
                                userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                            }

                            return currentStatus
                        }
                    }
                }
            }

            return payment.status
        } catch (e: Exception) {
            logger.error("Erro ao verificar status do pagamento: ${e.message}", e)
            return payment.status
        }
    }

    fun getUserPayments(userEmail: String): List<PaymentResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        return paymentRepository.findByUser(user).map { payment ->
            PaymentResponse(
                paymentId = payment.paymentId,
                preferenceId = payment.preferenceId,
                externalReference = payment.externalReference,
                status = payment.status,
                paymentUrl = "", // Não é necessário URL para histórico
                createdAt = payment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    @Transactional
    fun simulatePaymentApproval(externalReference: String, userEmail: String): String {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val payment = paymentRepository.findByExternalReference(externalReference)
            .orElseThrow { RuntimeException("Pagamento não encontrado com referência: $externalReference") }

        // Atualizar status para aprovado
        payment.status = "approved"
        payment.updatedAt = LocalDateTime.now()
        paymentRepository.save(payment)

        // Atualizar assinatura do usuário
        userService.updateSubscription(user.email, "SINGLE_PLAN", 1)

        return "approved"
    }
}
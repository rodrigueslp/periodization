package com.extrabox.periodization.service

import com.extrabox.periodization.entity.Payment
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.model.payment.PaymentRequest
import com.extrabox.periodization.model.payment.PaymentResponse
import com.extrabox.periodization.repository.*
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
import org.springframework.transaction.annotation.Isolation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class PaymentService(
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val strengthTrainingPlanRepository: StrengthTrainingPlanRepository,
    private val runningTrainingPlanRepository: RunningTrainingPlanRepository,
    private val bikeTrainingPlanRepository: BikeTrainingPlanRepository,
    private val userService: UserService,

    @Value("\${mercado-pago.access-token}")
    private val mercadoPagoAccessToken: String,

    @Value("\${app.backend-url}")
    private val backendUrl: String,

) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    private val client = OkHttpClient()
    private val objectMapper = ObjectMapper()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Cache para prevenir criação de pagamentos duplicados
    private val processingPayments = ConcurrentHashMap<String, Long>()

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun createPayment(request: PaymentRequest, userEmail: String): PaymentResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val lockKey = "${request.planId}-${userEmail}"
        val currentTime = System.currentTimeMillis()

        // Verificar se já está sendo processado (proteção contra duplo clique/requisições simultâneas)
        val existingProcessTime = processingPayments.putIfAbsent(lockKey, currentTime)
        if (existingProcessTime != null) {
            val timeDiff = currentTime - existingProcessTime
            if (timeDiff < 5000) { // 5 segundos de proteção
                logger.warn("Tentativa de criação de pagamento duplicado detectada para plano ${request.planId}. Diferença de tempo: ${timeDiff}ms")
                throw RuntimeException("Pagamento já está sendo processado. Aguarde alguns segundos.")
            } else {
                // Se passou muito tempo, substitui o lock
                processingPayments[lockKey] = currentTime
            }
        }

        try {
            // Buscar pagamentos existentes para este plano
            val existingPayments = paymentRepository.findByPlanId(request.planId)

            // Verificar se existe pagamento recente não-finalizado
            val recentPendingPayment = existingPayments
                .filter { it.status.lowercase() in listOf("pending", "in_process", "in_mediation") }
                .maxByOrNull { it.createdAt }

            if (recentPendingPayment != null) {
                val minutesAgo = java.time.Duration.between(recentPendingPayment.createdAt, LocalDateTime.now()).toMinutes()

                if (minutesAgo < 10) { // Se foi criado há menos de 10 minutos
                    logger.info("Retornando pagamento pendente existente para plano ${request.planId} (criado há $minutesAgo minutos)")

                    return PaymentResponse(
                        paymentId = recentPendingPayment.paymentId,
                        preferenceId = recentPendingPayment.preferenceId,
                        externalReference = recentPendingPayment.externalReference,
                        status = recentPendingPayment.status,
                        paymentUrl = "",
                        createdAt = recentPendingPayment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                        pixCopiaECola = null, // Poderia buscar novamente se necessário
                        qrCodeBase64 = null
                    )
                }
            }

            // Se chegou aqui, pode criar um novo pagamento
            return createNewPayment(request, user)

        } finally {
            // Remover o lock após 30 segundos (cleanup assíncrono)
            Thread {
                Thread.sleep(30000)
                processingPayments.remove(lockKey, currentTime)
            }.start()
        }
    }

    private fun createNewPayment(request: PaymentRequest, user: com.extrabox.periodization.entity.User): PaymentResponse {
        val externalReference = UUID.randomUUID().toString()

        try {
            // Verificar se o pagamento será por PIX
            val isPix = request.paymentMethod == "pix" || request.paymentMethod == null

            if (isPix) {
                return createPixPayment(request, user, externalReference)
            } else {
                return createCheckoutProPayment(request, user, externalReference)
            }
        } catch (e: Exception) {
            logger.error("Erro ao criar pagamento: ${e.message}", e)
            throw RuntimeException("Erro ao processar pagamento: ${e.message}")
        }
    }

    private fun createPixPayment(request: PaymentRequest, user: com.extrabox.periodization.entity.User, externalReference: String): PaymentResponse {
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

            val responseBody = response.body?.string()
                ?: throw RuntimeException("Corpo da resposta vazio")

            val paymentResult = objectMapper.readValue<Map<String, Any>>(responseBody)
            val paymentId = paymentResult["id"].toString()
            val status = paymentResult["status"] as String

            val pointOfInteraction = paymentResult["point_of_interaction"] as? Map<String, Any>
            val transactionData = pointOfInteraction?.get("transaction_data") as? Map<String, Any>

            val pixCopiaECola = transactionData?.get("qr_code") as? String
            val qrCodeBase64 = transactionData?.get("qr_code_base64") as? String

            // Persistir o pagamento no banco de dados
            val payment = Payment(
                paymentId = paymentId,
                preferenceId = "",
                externalReference = externalReference,
                amount = request.amount,
                status = status,
                description = request.description,
                user = user,
                planId = request.planId
            )

            paymentRepository.save(payment)

            logger.info("Pagamento PIX criado: $paymentId para plano ${request.planId}")

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
    }

    private fun createCheckoutProPayment(request: PaymentRequest, user: com.extrabox.periodization.entity.User, externalReference: String): PaymentResponse {
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
                "payment_methods": {
                    "excluded_payment_types": [
                        {"id": "ticket"}
                    ],
                    "installments": 1
                },
                "statement_descriptor": "Periodização CrossFit",
                "notification_url": "$backendUrl/api/payments/webhook"
            }
        """.trimIndent()

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

            val responseBody = response.body?.string()
                ?: throw RuntimeException("Corpo da resposta vazio")

            val preference = objectMapper.readValue<Map<String, Any>>(responseBody)
            val preferenceId = preference["id"] as String
            val initPoint = preference["init_point"] as String

            // Registrar o pagamento no banco
            val payment = Payment(
                paymentId = UUID.randomUUID().toString(),
                preferenceId = preferenceId,
                externalReference = externalReference,
                amount = request.amount,
                status = "pending",
                description = request.description,
                user = user,
                planId = request.planId
            )

            paymentRepository.save(payment)

            logger.info("Pagamento Checkout Pro criado: $preferenceId para plano ${request.planId}")

            return PaymentResponse(
                paymentId = payment.paymentId,
                preferenceId = preferenceId,
                externalReference = externalReference,
                status = "pending",
                paymentUrl = initPoint,
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAllPayments(): List<PaymentResponse> {
        return paymentRepository.findAll().map { payment ->
            PaymentResponse(
                paymentId = payment.paymentId,
                preferenceId = payment.preferenceId,
                externalReference = payment.externalReference,
                status = payment.status,
                paymentUrl = "",
                createdAt = payment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                amount = payment.amount,
                description = payment.description,
                userEmail = payment.user?.email,
                userName = payment.user?.fullName,
                planId = payment.planId,
                updatedAt = payment.updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    fun canSimulatePayment(userEmail: String): Boolean {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        return user.roles.any { it.name == "ROLE_PAYMENT_TESTER" || it.name == "ROLE_ADMIN" }
    }

    @Transactional
    fun processWebhook(data: Map<String, Any>): String {
        logger.info("Webhook recebido: $data")

        try {
            val type = data["type"] as? String ?: return "Tipo de notificação desconhecido"
            val dataInfo = data["data"] as? Map<String, Any> ?: return "Dados da notificação inválidos"

            when (type) {
                "payment" -> {
                    val paymentId = dataInfo["id"] as? String ?: return "ID de pagamento não encontrado"
                    return processPaymentWebhook(paymentId)
                }
                "merchant_order" -> {
                    logger.info("Merchant order recebido: $dataInfo")
                    return "Merchant order processado"
                }
                else -> {
                    logger.info("Tipo de webhook não processado: $type")
                    return "Webhook recebido mas não processado"
                }
            }
        } catch (e: Exception) {
            logger.error("Erro ao processar webhook: ${e.message}", e)
            return "Erro ao processar webhook: ${e.message}"
        }
    }

    private fun processPaymentWebhook(paymentId: String): String {
        try {
            val paymentRequest = Request.Builder()
                .url("https://api.mercadopago.com/v1/payments/$paymentId")
                .addHeader("Authorization", "Bearer $mercadoPagoAccessToken")
                .get()
                .build()

            client.newCall(paymentRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("Erro ao consultar pagamento $paymentId: ${response.code}")
                    return "Erro ao consultar pagamento: ${response.code}"
                }

                val paymentData = objectMapper.readValue<Map<String, Any>>(
                    response.body?.string() ?: return "Resposta vazia do Mercado Pago"
                )

                val status = paymentData["status"] as String
                val externalReference = paymentData["external_reference"] as? String
                    ?: return "Referência externa não encontrada no pagamento $paymentId"

                logger.info("Processando webhook - PaymentId: $paymentId, Status: $status, ExternalRef: $externalReference")

                // Buscar o pagamento no banco
                val payment = findPaymentByIdOrReference(paymentId, externalReference)
                    ?: return "Pagamento não encontrado no banco de dados - PaymentId: $paymentId, ExternalRef: $externalReference"

                logger.info("Pagamento encontrado no banco: ${payment.id}, Status atual: ${payment.status}")

                // Verificar se o status realmente mudou
                if (payment.status == status) {
                    logger.info("Status do pagamento $paymentId não mudou, mantendo: $status")
                    return "Status não alterado"
                }

                // Atualizar o pagamento
                updatePaymentStatus(payment, paymentId, status)

                // Processar aprovação se necessário
                if (status == "approved" && payment.status != "approved") {
                    processPaymentApproval(payment)
                }

                return "Pagamento $paymentId processado com sucesso: $status"
            }
        } catch (e: Exception) {
            logger.error("Erro ao processar webhook do pagamento $paymentId: ${e.message}", e)
            return "Erro ao processar pagamento: ${e.message}"
        }
    }

    private fun findPaymentByIdOrReference(paymentId: String, externalReference: String): Payment? {
        val paymentById = paymentRepository.findByPaymentId(paymentId).orElse(null)
        if (paymentById != null) return paymentById
        return paymentRepository.findByExternalReference(externalReference).orElse(null)
    }

    private fun updatePaymentStatus(payment: Payment, paymentId: String, status: String) {
        payment.paymentId = paymentId
        payment.status = status
        payment.updatedAt = LocalDateTime.now()
        paymentRepository.save(payment)
        logger.info("Status do pagamento atualizado: ${payment.externalReference} -> $status")
    }

    private fun processPaymentApproval(payment: Payment) {
        try {
            val user = payment.user
            if (user != null) {
                userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                logger.info("Assinatura ativada para usuário: ${user.email}")

                payment.planId?.let { planId ->
                    updatePlanStatus(planId, PlanStatus.PAYMENT_APPROVED)
                    logger.info("Status do plano $planId atualizado para PAYMENT_APPROVED")
                }
            } else {
                logger.error("Usuário não encontrado para o pagamento ${payment.id}")
            }
        } catch (e: Exception) {
            logger.error("Erro ao processar aprovação do pagamento ${payment.id}: ${e.message}", e)
        }
    }

    fun checkPaymentStatus(externalReference: String, userEmail: String): String {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val payment = paymentRepository.findByExternalReference(externalReference)
            .orElseThrow { RuntimeException("Pagamento não encontrado com referência: $externalReference") }

        if (payment.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Este pagamento não pertence ao usuário logado")
        }

        if (payment.status == "approved") {
            return payment.status
        }

        try {
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

                    if (currentStatus != payment.status) {
                        payment.status = currentStatus
                        payment.updatedAt = LocalDateTime.now()
                        paymentRepository.save(payment)

                        if (currentStatus == "approved") {
                            userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                            payment.planId?.let { planId ->
                                updatePlanStatus(planId, PlanStatus.PAYMENT_APPROVED)
                            }
                        }
                    }

                    return currentStatus
                }
            } else if (payment.preferenceId.isNotEmpty()) {
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

                        payment.paymentId = newPaymentId
                        payment.status = currentStatus
                        payment.updatedAt = LocalDateTime.now()
                        paymentRepository.save(payment)

                        if (currentStatus == "approved") {
                            userService.updateSubscription(user.email, "SINGLE_PLAN", 1)
                            payment.planId?.let { planId ->
                                updatePlanStatus(planId, PlanStatus.PAYMENT_APPROVED)
                            }
                        }

                        return currentStatus
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
                paymentUrl = "",
                createdAt = payment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                pixCopiaECola = null,
                qrCodeBase64 = null,
                amount = payment.amount,
                description = payment.description,
                planId = payment.planId
            )
        }
    }


    @Transactional
    fun updatePlanStatus(planId: String, status: PlanStatus) {
        try {
            // Primeiro tenta encontrar nos planos de CrossFit
            val trainingPlanOptional = trainingPlanRepository.findByPlanId(planId)
            if (trainingPlanOptional.isPresent) {
                val trainingPlan = trainingPlanOptional.get()
                trainingPlan.status = status
                trainingPlanRepository.save(trainingPlan)
                logger.info("Status do plano de CrossFit $planId atualizado para $status")
                return
            }

            // Se não encontrou, tenta nos planos de musculação
            val strengthPlanOptional = strengthTrainingPlanRepository.findByPlanId(planId)
            if (strengthPlanOptional.isPresent) {
                val strengthPlan = strengthPlanOptional.get()
                strengthPlan.status = status
                strengthTrainingPlanRepository.save(strengthPlan)
                logger.info("Status do plano de musculação $planId atualizado para $status")
                return
            }

            // Se não encontrou, tenta nos planos de corrida
            val runningPlanOptional = runningTrainingPlanRepository.findByPlanId(planId)
            if (runningPlanOptional.isPresent) {
                val runningPlan = runningPlanOptional.get()
                runningPlan.status = status
                runningTrainingPlanRepository.save(runningPlan)
                logger.info("Status do plano de corrida $planId atualizado para $status")
                return
            }

            // Se não encontrou, tenta nos planos de bike
            val bikePlanOptional = bikeTrainingPlanRepository.findByPlanId(planId)
            if (bikePlanOptional.isPresent) {
                val bikePlan = bikePlanOptional.get()
                bikePlan.status = status
                bikeTrainingPlanRepository.save(bikePlan)
                logger.info("Status do plano de bike $planId atualizado para $status")
                return
            }

            // Se não encontrou em nenhum lugar, lança exceção
            throw RuntimeException("Plano não encontrado com o ID: $planId")

        } catch (e: Exception) {
            logger.error("Erro ao atualizar status do plano $planId: ${e.message}", e)
            throw e
        }
    }

    @Transactional
    fun simulatePaymentApproval(externalReference: String, userEmail: String): String {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (!canSimulatePayment(userEmail)) {
            throw AccessDeniedException("Usuário não tem permissão para simular pagamentos")
        }

        val payment = paymentRepository.findByExternalReference(externalReference)
            .orElseThrow { RuntimeException("Pagamento não encontrado com referência: $externalReference") }

        payment.status = "approved"
        payment.updatedAt = LocalDateTime.now()
        paymentRepository.save(payment)

        userService.updateSubscription(user.email, "SINGLE_PLAN", 1)

        payment.planId?.let { planId ->
            updatePlanStatus(planId, PlanStatus.PAYMENT_APPROVED)
        }

        return "approved"
    }

    // Método auxiliar para debug se necessário
    fun getPaymentsByPlanId(planId: String): List<PaymentResponse> {
        val payments = paymentRepository.findByPlanId(planId)
        return payments.map { payment ->
            PaymentResponse(
                paymentId = payment.paymentId,
                preferenceId = payment.preferenceId,
                externalReference = payment.externalReference,
                status = payment.status,
                paymentUrl = "",
                createdAt = payment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                amount = payment.amount,
                description = payment.description,
                planId = payment.planId
            )
        }
    }

    @Transactional(readOnly = true)
    fun recoverPaymentInfo(planId: String, userEmail: String): PaymentResponse {
        // Validar usuário
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Buscar pagamentos para este plano
        val payments = paymentRepository.findByPlanId(planId)

        if (payments.isEmpty()) {
            throw RuntimeException("Nenhum pagamento encontrado para o plano: $planId")
        }

        val payment = payments.first()

        // Verificar permissões
        validatePaymentAccess(payment, user)

        // Verificar status atual no Mercado Pago e atualizar se necessário
        val currentStatus = checkAndUpdatePaymentStatus(payment, userEmail)

        return PaymentResponse(
            paymentId = payment.paymentId,
            preferenceId = payment.preferenceId,
            externalReference = payment.externalReference,
            status = currentStatus,
            paymentUrl = "",
            createdAt = payment.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
            amount = payment.amount,
            description = payment.description,
            planId = planId,
            updatedAt = payment.updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME)
        )
    }

    private fun validatePaymentAccess(payment: Payment, user: com.extrabox.periodization.entity.User) {
        val isOwner = payment.user?.id == user.id
        val isAdmin = user.roles.any { it.name == "ROLE_ADMIN" }

        if (!isOwner && !isAdmin) {
            throw AccessDeniedException("Acesso negado. Este pagamento não pertence ao usuário logado.")
        }
    }

    private fun checkAndUpdatePaymentStatus(payment: Payment, userEmail: String): String {
        return try {
            // Verificar status atual no Mercado Pago
            val currentStatus = checkPaymentStatus(payment.externalReference, userEmail)

            // Se o status mudou, atualizar no banco
            if (payment.status != currentStatus) {
                logger.info("Status do pagamento ${payment.externalReference} mudou de ${payment.status} para $currentStatus")

                payment.status = currentStatus
                payment.updatedAt = LocalDateTime.now()
                paymentRepository.save(payment)

                // Se foi aprovado, processar aprovação
                if (currentStatus == "approved") {
                    processPaymentApproval(payment)
                }
            }

            currentStatus
        } catch (e: Exception) {
            logger.warn("Não foi possível verificar status atual do pagamento ${payment.externalReference}: ${e.message}")
            // Retorna o status que está no banco
            payment.status
        }
    }
}

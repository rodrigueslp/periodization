package com.extrabox.periodization.service

import com.extrabox.periodization.entity.BenchmarkData
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.entity.TrainingPlan
import com.extrabox.periodization.messaging.PlanGenerationProducer
import com.extrabox.periodization.model.PlanDetailsResponse
import com.extrabox.periodization.model.PlanRequest
import com.extrabox.periodization.model.PlanResponse
import com.extrabox.periodization.repository.BenchmarkDataRepository
import com.extrabox.periodization.repository.TrainingPlanRepository
import com.extrabox.periodization.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class PeriodizationService(
    private val anthropicService: AnthropicService,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val benchmarkDataRepository: BenchmarkDataRepository,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository,
    private val planGenerationProducer: PlanGenerationProducer
) {
    private val logger = LoggerFactory.getLogger(PeriodizationService::class.java)

    /**
     * Cria um novo plano sem conteúdo, apenas com os dados básicos do atleta e status PAYMENT_PENDING
     */
    @Transactional
    fun createPendingPlan(request: PlanRequest, userEmail: String): PlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val planId = UUID.randomUUID().toString()

        val startDate = if (request.startDate != null && request.startDate != "") {
            LocalDate.parse(request.startDate)
        } else {
            LocalDate.now()
        }

        val endDate = startDate.plusWeeks(request.planDuration.toLong())

        // Criar plano pendente de pagamento
        val trainingPlan = TrainingPlan(
            planId = planId,
            athleteName = request.athleteData.nome,
            athleteAge = request.athleteData.idade,
            athleteWeight = request.athleteData.peso,
            athleteHeight = request.athleteData.altura,
            experienceLevel = request.athleteData.experiencia,
            trainingGoal = request.athleteData.objetivo,
            availability = request.athleteData.disponibilidade,
            injuries = request.athleteData.lesoes,
            trainingHistory = request.athleteData.historico,
            detailedGoal = request.athleteData.objetivoDetalhado,
            planDuration = request.planDuration,
            planContent = "", // Vazio até ser gerado
            excelFilePath = "", // Vazio até ser gerado
            user = user,
            status = PlanStatus.PAYMENT_PENDING,
            startDate = startDate,
            endDate = endDate
        )
        trainingPlanRepository.save(trainingPlan)

        // Salvar benchmarks se disponíveis
        request.athleteData.benchmarks?.let { benchmarks ->
            val benchmarkData = BenchmarkData(
                planId = planId,
                backSquat = benchmarks.backSquat,
                deadlift = benchmarks.deadlift,
                clean = benchmarks.clean,
                snatch = benchmarks.snatch,
                fran = benchmarks.fran,
                grace = benchmarks.grace
            )
            benchmarkDataRepository.save(benchmarkData)
        }

        return PlanResponse(
            planId = planId,
            message = "Plano pendente de pagamento criado com sucesso"
        )
    }

    /**
     * Inicia a geração assíncrona de um plano que já foi aprovado para pagamento
     */
    @Transactional
    fun generateApprovedPlan(planId: String, userEmail: String): PlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Buscar o plano pelo ID
        val trainingPlan = trainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário
        if (trainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Este plano não pertence ao usuário logado")
        }

        // Verificar se o plano está no estado correto para ser gerado
        if (trainingPlan.status != PlanStatus.PAYMENT_APPROVED && trainingPlan.status != PlanStatus.FAILED) {
            throw IllegalStateException("Este plano não está aprovado para geração. Status atual: ${trainingPlan.status}")
        }

        try {
            // Atualizar status para QUEUED
            trainingPlan.status = PlanStatus.QUEUED
            trainingPlanRepository.save(trainingPlan)

            // Enviar mensagem para o RabbitMQ
            logger.info("Enviando plano $planId para a fila de geração")
            planGenerationProducer.sendPlanGenerationRequest(planId, userEmail)

            return PlanResponse(
                planId = planId,
                message = "Plano de treinamento enviado para geração assíncrona"
            )
        } catch (e: Exception) {
            logger.error("Erro ao enfileirar plano para geração: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            trainingPlan.status = PlanStatus.FAILED
            trainingPlanRepository.save(trainingPlan)
            throw e
        }
    }

    /**
     * Método legado para compatibilidade - agora divide o processo em duas etapas
     */
    @Transactional
    fun generatePlan(request: PlanRequest, userEmail: String): PlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Verificar se o usuário tem uma assinatura ativa ou é administrador
        val hasActiveSubscription = user.subscriptionExpiry?.isAfter(LocalDateTime.now()) ?: false

        if (!hasActiveSubscription && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("É necessário um pagamento para gerar planos de treinamento")
        }

        // Criar plano pendente
        val pendingResponse = createPendingPlan(request, userEmail)
        val planId = pendingResponse.planId

        // Simular aprovação imediata (para compatibilidade)
        val trainingPlan = trainingPlanRepository.findByPlanId(planId).get()
        trainingPlan.status = PlanStatus.PAYMENT_APPROVED
        trainingPlanRepository.save(trainingPlan)

        // Gerar o plano aprovado
        return generateApprovedPlan(planId, userEmail)
    }

    fun getPlanExcel(planId: String, userEmail: String): ByteArray {
        val trainingPlan = trainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (trainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        return fileStorageService.loadFile(planId)
    }

    fun getPlanContent(planId: String, userEmail: String): PlanDetailsResponse {
        val trainingPlan = trainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (trainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        val benchmarks = benchmarkDataRepository.findByPlanId(planId).orElse(null)

        // Verificar se o plano pode ser gerado
        // (se o pagamento foi aprovado mas o plano ainda não foi gerado com sucesso)
        val canGenerate = trainingPlan.status == PlanStatus.PAYMENT_APPROVED ||
                trainingPlan.status == PlanStatus.FAILED

        return PlanDetailsResponse(
            planId = trainingPlan.planId,
            athleteName = trainingPlan.athleteName,
            athleteAge = trainingPlan.athleteAge,
            athleteWeight = trainingPlan.athleteWeight,
            athleteHeight = trainingPlan.athleteHeight,
            experienceLevel = trainingPlan.experienceLevel,
            trainingGoal = trainingPlan.trainingGoal,
            availability = trainingPlan.availability,
            injuries = trainingPlan.injuries,
            trainingHistory = trainingPlan.trainingHistory,
            planDuration = trainingPlan.planDuration,
            planContent = trainingPlan.planContent,
            createdAt = trainingPlan.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            benchmarks = benchmarks?.let {
                mapOf(
                    "backSquat" to it.backSquat,
                    "deadlift" to it.deadlift,
                    "clean" to it.clean,
                    "snatch" to it.snatch,
                    "fran" to it.fran,
                    "grace" to it.grace
                )
            },
            status = trainingPlan.status,
            canGenerate = canGenerate,
            startDate = trainingPlan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            endDate = trainingPlan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR")))
        )
    }

    fun getUserPlans(userEmail: String): List<PlanDetailsResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val plans = if (user.roles.any { it.name == "ROLE_ADMIN" }) {
            // Administradores podem ver todos os planos
            trainingPlanRepository.findTop10ByOrderByCreatedAtDesc()
        } else {
            // Usuários normais só veem seus próprios planos
            trainingPlanRepository.findByUserOrderByCreatedAtDesc(user)
        }

        return plans.map { plan ->
            val benchmarks = benchmarkDataRepository.findByPlanId(plan.planId).orElse(null)

            // Verificar se o plano pode ser gerado (se o pagamento foi aprovado mas o plano ainda não foi gerado)
            val canGenerate = plan.status == PlanStatus.PAYMENT_APPROVED ||
                    plan.status == PlanStatus.FAILED

            PlanDetailsResponse(
                planId = plan.planId,
                athleteName = plan.athleteName,
                athleteAge = plan.athleteAge,
                athleteWeight = plan.athleteWeight,
                athleteHeight = plan.athleteHeight,
                experienceLevel = plan.experienceLevel,
                trainingGoal = plan.trainingGoal,
                availability = plan.availability,
                injuries = plan.injuries,
                trainingHistory = plan.trainingHistory,
                planDuration = plan.planDuration,
                planContent = plan.planContent,
                createdAt = plan.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withLocale(Locale.of("pt", "BR"))),
                benchmarks = benchmarks?.let {
                    mapOf(
                        "backSquat" to it.backSquat,
                        "deadlift" to it.deadlift,
                        "clean" to it.clean,
                        "snatch" to it.snatch,
                        "fran" to it.fran,
                        "grace" to it.grace
                    )
                },
                status = plan.status,
                canGenerate = canGenerate,
                startDate = plan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withLocale(Locale.of("pt", "BR"))),
                endDate = plan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withLocale(Locale.of("pt", "BR")))
            )
        }
    }
}

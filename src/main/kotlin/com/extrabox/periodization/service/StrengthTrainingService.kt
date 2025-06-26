package com.extrabox.periodization.service

import com.extrabox.periodization.entity.StrengthTrainingPlan
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.messaging.PlanGenerationProducer
import com.extrabox.periodization.messaging.StrengthPlanGenerationProducer
import com.extrabox.periodization.model.*
import com.extrabox.periodization.repository.StrengthTrainingPlanRepository
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
class StrengthTrainingService(
    private val strengthAnthropicService: StrengthAnthropicService,
    private val strengthTrainingPlanRepository: StrengthTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository,
    private val planGenerationProducer: PlanGenerationProducer,
    private val pdfGenerationService: PdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(StrengthTrainingService::class.java)

    /**
     * Cria um novo plano de musculação sem conteúdo, apenas com os dados básicos do atleta e status PAYMENT_PENDING
     */
    @Transactional
    fun createPendingPlan(request: StrengthPlanRequest, userEmail: String): StrengthPlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val planId = UUID.randomUUID().toString()

        val startDate = if (request.startDate != null && request.startDate != "") {
            LocalDate.parse(request.startDate)
        } else {
            val today = LocalDate.now()
            // Verifica se hoje é segunda-feira (DayOfWeek.MONDAY tem valor 1)
            if (today.dayOfWeek.value == 1) {
                today
            } else {
                // Calcula a próxima segunda-feira
                today.plusDays((8 - today.dayOfWeek.value).toLong())
            }
        }

        // Cálculo para endDate - termina no final da última semana
        val endDate = startDate.plusWeeks(request.planDuration.toLong())

        // Criar plano pendente de pagamento
        val strengthTrainingPlan = StrengthTrainingPlan(
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
            trainingFocus = request.athleteData.trainingFocus,
            equipmentAvailable = request.athleteData.equipmentAvailable,
            sessionsPerWeek = request.athleteData.sessionsPerWeek,
            sessionDuration = request.athleteData.sessionDuration,
            planContent = "", // Vazio até ser gerado
            excelFilePath = "", // Vazio até ser gerado
            pdfFilePath = "", // Vazio até ser gerado
            user = user,
            status = PlanStatus.PAYMENT_PENDING,
            startDate = startDate,
            endDate = endDate
        )
        strengthTrainingPlanRepository.save(strengthTrainingPlan)

        return StrengthPlanResponse(
            planId = planId,
            message = "Plano de musculação pendente de pagamento criado com sucesso"
        )
    }

    /**
     * Inicia a geração assíncrona de um plano de musculação que já foi aprovado para pagamento
     */
    @Transactional
    fun generateApprovedPlan(planId: String, userEmail: String): StrengthPlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Buscar o plano pelo ID
        val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário
        if (strengthTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Este plano não pertence ao usuário logado")
        }

        // Verificar se o plano está no estado correto para ser gerado
        if (strengthTrainingPlan.status != PlanStatus.PAYMENT_APPROVED && strengthTrainingPlan.status != PlanStatus.FAILED) {
            throw IllegalStateException("Este plano não está aprovado para geração. Status atual: ${strengthTrainingPlan.status}")
        }

        try {
            // Atualizar status para QUEUED
            strengthTrainingPlan.status = PlanStatus.QUEUED
            strengthTrainingPlanRepository.save(strengthTrainingPlan)

            // Enviar mensagem para o RabbitMQ
            logger.info("Enviando plano de musculação $planId para a fila de geração")
            planGenerationProducer.sendPlanGenerationRequest(planId, userEmail, PlanType.STRENGTH)

            return StrengthPlanResponse(
                planId = planId,
                message = "Plano de musculação enviado para geração assíncrona"
            )
        } catch (e: Exception) {
            logger.error("Erro ao enfileirar plano de musculação para geração: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            strengthTrainingPlan.status = PlanStatus.FAILED
            strengthTrainingPlanRepository.save(strengthTrainingPlan)
            throw e
        }
    }

    /**
     * Método legado para compatibilidade - agora divide o processo em duas etapas
     */
    @Transactional
    fun generatePlan(request: StrengthPlanRequest, userEmail: String): StrengthPlanResponse {
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
        val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(planId).get()
        strengthTrainingPlan.status = PlanStatus.PAYMENT_APPROVED
        strengthTrainingPlanRepository.save(strengthTrainingPlan)

        // Gerar o plano aprovado
        return generateApprovedPlan(planId, userEmail)
    }

    /**
     * Obtém o arquivo Excel do plano
     */
    fun getPlanExcel(planId: String, userEmail: String): ByteArray {
        val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (strengthTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        return fileStorageService.loadFile(planId)
    }

    /**
     * Obtém o conteúdo do plano
     */
    fun getPlanContent(planId: String, userEmail: String): StrengthPlanDetailsResponse {
        val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (strengthTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        // Verificar se o plano pode ser gerado
        // (se o pagamento foi aprovado mas o plano ainda não foi gerado com sucesso)
        val canGenerate = strengthTrainingPlan.status == PlanStatus.PAYMENT_APPROVED ||
                strengthTrainingPlan.status == PlanStatus.FAILED

        return StrengthPlanDetailsResponse(
            planId = strengthTrainingPlan.planId,
            athleteName = strengthTrainingPlan.athleteName,
            athleteAge = strengthTrainingPlan.athleteAge,
            athleteWeight = strengthTrainingPlan.athleteWeight,
            athleteHeight = strengthTrainingPlan.athleteHeight,
            experienceLevel = strengthTrainingPlan.experienceLevel,
            trainingGoal = strengthTrainingPlan.trainingGoal,
            availability = strengthTrainingPlan.availability,
            injuries = strengthTrainingPlan.injuries,
            trainingHistory = strengthTrainingPlan.trainingHistory,
            planDuration = strengthTrainingPlan.planDuration,
            planContent = strengthTrainingPlan.planContent,
            trainingFocus = strengthTrainingPlan.trainingFocus,
            equipmentAvailable = strengthTrainingPlan.equipmentAvailable,
            sessionsPerWeek = strengthTrainingPlan.sessionsPerWeek,
            sessionDuration = strengthTrainingPlan.sessionDuration,
            pdfFilePath = strengthTrainingPlan.pdfFilePath,
            excelFilePath = strengthTrainingPlan.excelFilePath,
            createdAt = strengthTrainingPlan.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            status = strengthTrainingPlan.status,
            canGenerate = canGenerate,
            startDate = strengthTrainingPlan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            endDate = strengthTrainingPlan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR")))
        )
    }

    /**
     * Obtém o PDF do plano
     */
    fun getPlanPdf(planId: String, userEmail: String): ByteArray {
        val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (strengthTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        try {
            // Tenta carregar o arquivo PDF existente
            return fileStorageService.loadPdfFile(planId)
        } catch (e: Exception) {
            logger.info("PDF não encontrado para o plano $planId. Tentando gerar um novo PDF.")

            // Verifica se o plano tem conteúdo para gerar um novo PDF
            if (strengthTrainingPlan.planContent.isBlank()) {
                throw RuntimeException("Não é possível gerar o PDF. O plano não possui conteúdo.")
            }

            // Constrói o objeto AthleteData com base no plano salvo
            // Note que estamos adaptando para usar o serviço existente
            val athleteData = AthleteData(
                nome = strengthTrainingPlan.athleteName,
                idade = strengthTrainingPlan.athleteAge,
                peso = strengthTrainingPlan.athleteWeight,
                altura = strengthTrainingPlan.athleteHeight,
                experiencia = strengthTrainingPlan.experienceLevel,
                objetivo = strengthTrainingPlan.trainingGoal,
                disponibilidade = strengthTrainingPlan.availability,
                lesoes = strengthTrainingPlan.injuries,
                historico = strengthTrainingPlan.trainingHistory,
                objetivoDetalhado = strengthTrainingPlan.detailedGoal,
                treinoPrincipal = true,
                periodoTreino = null
            )

            // Gera novo PDF usando o conteúdo existente
            val pdfData = pdfGenerationService.generatePdf(athleteData, strengthTrainingPlan.planContent)

            // Salva o novo PDF gerado
            val pdfFilePath = fileStorageService.savePdfFile(planId, pdfData)

            // Atualiza o caminho do PDF no plano
            strengthTrainingPlan.pdfFilePath = pdfFilePath
            strengthTrainingPlanRepository.save(strengthTrainingPlan)

            logger.info("Novo PDF gerado com sucesso para o plano $planId")

            return pdfData
        }
    }

    /**
     * Lista todos os planos de musculação do usuário
     */
    fun getUserPlans(userEmail: String): List<StrengthPlanDetailsResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val plans = strengthTrainingPlanRepository.findByUserOrderByCreatedAtDesc(user)

        return plans.map { plan ->
            // Verificar se o plano pode ser gerado
            val canGenerate = plan.status == PlanStatus.PAYMENT_APPROVED ||
                    plan.status == PlanStatus.FAILED

            StrengthPlanDetailsResponse(
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
                trainingFocus = plan.trainingFocus,
                equipmentAvailable = plan.equipmentAvailable,
                sessionsPerWeek = plan.sessionsPerWeek,
                sessionDuration = plan.sessionDuration,
                pdfFilePath = plan.pdfFilePath,
                excelFilePath = plan.excelFilePath,
                createdAt = plan.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withLocale(Locale.of("pt", "BR"))),
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
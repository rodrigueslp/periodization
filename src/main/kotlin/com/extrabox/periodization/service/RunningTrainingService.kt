package com.extrabox.periodization.service

import com.extrabox.periodization.entity.RunningTrainingPlan
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.messaging.PlanGenerationProducer
import com.extrabox.periodization.model.*
import com.extrabox.periodization.repository.RunningTrainingPlanRepository
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
class RunningTrainingService(
    private val runningAnthropicService: RunningAnthropicService,
    private val runningTrainingPlanRepository: RunningTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository,
    private val planGenerationProducer: PlanGenerationProducer,
    private val runningPdfGenerationService: RunningPdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(RunningTrainingService::class.java)

    /**
     * Cria um novo plano de corrida sem conteúdo, apenas com os dados básicos do atleta e status PAYMENT_PENDING
     */
    @Transactional
    fun createPendingPlan(request: RunningPlanRequest, userEmail: String): RunningPlanResponse {
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
        val runningTrainingPlan = RunningTrainingPlan(
            planId = planId,
            athleteName = request.athleteData.nome,
            athleteAge = request.athleteData.idade,
            athleteWeight = request.athleteData.peso,
            athleteHeight = request.athleteData.altura,
            experienceLevel = request.athleteData.experiencia,
            trainingGoal = request.athleteData.objetivo,
            diasDisponiveis = request.athleteData.diasDisponiveis,
            volumeSemanalAtual = request.athleteData.volumeSemanalAtual,
            paceAtual5k = request.athleteData.paceAtual5k,
            paceAtual10k = request.athleteData.paceAtual10k,
            melhorTempo5k = request.athleteData.melhorTempo5k,
            melhorTempo10k = request.athleteData.melhorTempo10k,
            melhorTempo21k = request.athleteData.melhorTempo21k,
            melhorTempo42k = request.athleteData.melhorTempo42k,
            tempoObjetivo = request.athleteData.tempoObjetivo,
            dataProva = request.athleteData.dataProva,
            historicoLesoes = request.athleteData.historicoLesoes,
            experienciaAnterior = request.athleteData.experienciaAnterior,
            preferenciaTreino = request.athleteData.preferenciaTreino,
            localTreino = request.athleteData.localTreino,
            equipamentosDisponiveis = request.athleteData.equipamentosDisponiveis,
            planDuration = request.planDuration,
            planContent = "", // Vazio até ser gerado
            excelFilePath = "", // Vazio até ser gerado
            pdfFilePath = "", // Vazio até ser gerado
            user = user,
            status = PlanStatus.PAYMENT_PENDING,
            startDate = startDate,
            endDate = endDate
        )
        runningTrainingPlanRepository.save(runningTrainingPlan)

        return RunningPlanResponse(
            planId = planId,
            message = "Plano de corrida pendente de pagamento criado com sucesso"
        )
    }

    /**
     * Inicia a geração assíncrona de um plano de corrida que já foi aprovado para pagamento
     */
    @Transactional
    fun generateApprovedPlan(planId: String, userEmail: String): RunningPlanResponse {
        // Verificar se o usuário existe
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Buscar o plano pelo ID
        val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário
        if (runningTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Este plano não pertence ao usuário logado")
        }

        // Verificar se o plano está no estado correto para ser gerado
        if (runningTrainingPlan.status != PlanStatus.PAYMENT_APPROVED && runningTrainingPlan.status != PlanStatus.FAILED) {
            throw IllegalStateException("Este plano não está aprovado para geração. Status atual: ${runningTrainingPlan.status}")
        }

        try {
            // Atualizar status para QUEUED
            runningTrainingPlan.status = PlanStatus.QUEUED
            runningTrainingPlanRepository.save(runningTrainingPlan)

            // Enviar mensagem para o RabbitMQ
            logger.info("Enviando plano de corrida $planId para a fila de geração")
            planGenerationProducer.sendPlanGenerationRequest(planId, userEmail, PlanType.RUNNING)

            return RunningPlanResponse(
                planId = planId,
                message = "Plano de corrida enviado para geração assíncrona"
            )
        } catch (e: Exception) {
            logger.error("Erro ao enfileirar plano de corrida para geração: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            runningTrainingPlan.status = PlanStatus.FAILED
            runningTrainingPlanRepository.save(runningTrainingPlan)
            throw e
        }
    }

    /**
     * Método legado para compatibilidade - agora divide o processo em duas etapas
     */
    @Transactional
    fun generatePlan(request: RunningPlanRequest, userEmail: String): RunningPlanResponse {
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
        val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(planId).get()
        runningTrainingPlan.status = PlanStatus.PAYMENT_APPROVED
        runningTrainingPlanRepository.save(runningTrainingPlan)

        // Gerar o plano aprovado
        return generateApprovedPlan(planId, userEmail)
    }

    /**
     * Obtém o arquivo Excel do plano
     */
    fun getPlanExcel(planId: String, userEmail: String): ByteArray {
        val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (runningTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        return fileStorageService.loadFile(planId)
    }

    /**
     * Obtém o conteúdo do plano
     */
    fun getPlanContent(planId: String, userEmail: String): RunningPlanDetailsResponse {
        val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (runningTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        // Verificar se o plano pode ser gerado
        // (se o pagamento foi aprovado mas o plano ainda não foi gerado com sucesso)
        val canGenerate = runningTrainingPlan.status == PlanStatus.PAYMENT_APPROVED ||
                runningTrainingPlan.status == PlanStatus.FAILED

        return RunningPlanDetailsResponse(
            planId = runningTrainingPlan.planId,
            athleteName = runningTrainingPlan.athleteName,
            athleteAge = runningTrainingPlan.athleteAge,
            athleteWeight = runningTrainingPlan.athleteWeight,
            athleteHeight = runningTrainingPlan.athleteHeight,
            experienceLevel = runningTrainingPlan.experienceLevel,
            trainingGoal = runningTrainingPlan.trainingGoal,
            diasDisponiveis = runningTrainingPlan.diasDisponiveis,
            volumeSemanalAtual = runningTrainingPlan.volumeSemanalAtual,
            paceAtual5k = runningTrainingPlan.paceAtual5k,
            paceAtual10k = runningTrainingPlan.paceAtual10k,
            melhorTempo5k = runningTrainingPlan.melhorTempo5k,
            melhorTempo10k = runningTrainingPlan.melhorTempo10k,
            melhorTempo21k = runningTrainingPlan.melhorTempo21k,
            melhorTempo42k = runningTrainingPlan.melhorTempo42k,
            tempoObjetivo = runningTrainingPlan.tempoObjetivo,
            dataProva = runningTrainingPlan.dataProva,
            historicoLesoes = runningTrainingPlan.historicoLesoes,
            experienciaAnterior = runningTrainingPlan.experienciaAnterior,
            preferenciaTreino = runningTrainingPlan.preferenciaTreino,
            localTreino = runningTrainingPlan.localTreino,
            equipamentosDisponiveis = runningTrainingPlan.equipamentosDisponiveis,
            planDuration = runningTrainingPlan.planDuration,
            planContent = runningTrainingPlan.planContent,
            pdfFilePath = runningTrainingPlan.pdfFilePath,
            excelFilePath = runningTrainingPlan.excelFilePath,
            createdAt = runningTrainingPlan.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            status = runningTrainingPlan.status,
            canGenerate = canGenerate,
            startDate = runningTrainingPlan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR"))),
            endDate = runningTrainingPlan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withLocale(Locale.of("pt", "BR")))
        )
    }

    /**
     * Obtém o PDF do plano
     */
    fun getPlanPdf(planId: String, userEmail: String): ByteArray {
        val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (runningTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw AccessDeniedException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        try {
            // Tenta carregar o arquivo PDF existente
            return fileStorageService.loadPdfFile(planId)
        } catch (e: Exception) {
            logger.info("PDF não encontrado para o plano $planId. Tentando gerar um novo PDF.")

            // Verifica se o plano tem conteúdo para gerar um novo PDF
            if (runningTrainingPlan.planContent.isBlank()) {
                throw RuntimeException("Não é possível gerar o PDF. O plano não possui conteúdo.")
            }

            // Constrói o objeto RunningAthleteData com base no plano salvo
            val athleteData = RunningAthleteData(
                nome = runningTrainingPlan.athleteName,
                idade = runningTrainingPlan.athleteAge,
                peso = runningTrainingPlan.athleteWeight,
                altura = runningTrainingPlan.athleteHeight,
                experiencia = runningTrainingPlan.experienceLevel,
                objetivo = runningTrainingPlan.trainingGoal,
                diasDisponiveis = runningTrainingPlan.diasDisponiveis,
                volumeSemanalAtual = runningTrainingPlan.volumeSemanalAtual,
                paceAtual5k = runningTrainingPlan.paceAtual5k,
                paceAtual10k = runningTrainingPlan.paceAtual10k,
                melhorTempo5k = runningTrainingPlan.melhorTempo5k,
                melhorTempo10k = runningTrainingPlan.melhorTempo10k,
                melhorTempo21k = runningTrainingPlan.melhorTempo21k,
                melhorTempo42k = runningTrainingPlan.melhorTempo42k,
                tempoObjetivo = runningTrainingPlan.tempoObjetivo,
                dataProva = runningTrainingPlan.dataProva,
                historicoLesoes = runningTrainingPlan.historicoLesoes,
                experienciaAnterior = runningTrainingPlan.experienciaAnterior,
                preferenciaTreino = runningTrainingPlan.preferenciaTreino,
                localTreino = runningTrainingPlan.localTreino,
                equipamentosDisponiveis = runningTrainingPlan.equipamentosDisponiveis
            )

            // Gera novo PDF usando o conteúdo existente
            val pdfData = runningPdfGenerationService.generatePdf(athleteData, runningTrainingPlan.planContent)

            // Salva o novo PDF gerado
            val pdfFilePath = fileStorageService.savePdfFile(planId, pdfData)

            // Atualiza o caminho do PDF no plano
            runningTrainingPlan.pdfFilePath = pdfFilePath
            runningTrainingPlanRepository.save(runningTrainingPlan)

            logger.info("Novo PDF gerado com sucesso para o plano $planId")

            return pdfData
        }
    }

    /**
     * Lista todos os planos de corrida do usuário
     */
    fun getUserPlans(userEmail: String): List<RunningPlanDetailsResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val plans = runningTrainingPlanRepository.findByUserOrderByCreatedAtDesc(user)

        return plans.map { plan ->
            // Verificar se o plano pode ser gerado
            val canGenerate = plan.status == PlanStatus.PAYMENT_APPROVED ||
                    plan.status == PlanStatus.FAILED

            RunningPlanDetailsResponse(
                planId = plan.planId,
                athleteName = plan.athleteName,
                athleteAge = plan.athleteAge,
                athleteWeight = plan.athleteWeight,
                athleteHeight = plan.athleteHeight,
                experienceLevel = plan.experienceLevel,
                trainingGoal = plan.trainingGoal,
                diasDisponiveis = plan.diasDisponiveis,
                volumeSemanalAtual = plan.volumeSemanalAtual,
                paceAtual5k = plan.paceAtual5k,
                paceAtual10k = plan.paceAtual10k,
                melhorTempo5k = plan.melhorTempo5k,
                melhorTempo10k = plan.melhorTempo10k,
                melhorTempo21k = plan.melhorTempo21k,
                melhorTempo42k = plan.melhorTempo42k,
                tempoObjetivo = plan.tempoObjetivo,
                dataProva = plan.dataProva,
                historicoLesoes = plan.historicoLesoes,
                experienciaAnterior = plan.experienciaAnterior,
                preferenciaTreino = plan.preferenciaTreino,
                localTreino = plan.localTreino,
                equipamentosDisponiveis = plan.equipamentosDisponiveis,
                planDuration = plan.planDuration,
                planContent = plan.planContent,
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

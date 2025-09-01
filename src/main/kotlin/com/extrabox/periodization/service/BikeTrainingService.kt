package com.extrabox.periodization.service

import com.extrabox.periodization.entity.BikeTrainingPlan
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.messaging.PlanGenerationProducer
import com.extrabox.periodization.model.*
import com.extrabox.periodization.repository.BikeTrainingPlanRepository
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
class BikeTrainingService(
    private val bikeAnthropicService: BikeAnthropicService,
    private val bikeTrainingPlanRepository: BikeTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository,
    private val planGenerationProducer: PlanGenerationProducer,
    private val bikePdfGenerationService: BikePdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(BikeTrainingService::class.java)

    /**
     * Cria um novo plano de bike sem conteúdo, apenas com os dados básicos do atleta e status PAYMENT_PENDING
     */
    @Transactional
    fun createPendingPlan(request: BikePlanRequest, userEmail: String): BikePlanResponse {
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

        val endDate = startDate.plusWeeks(request.planDuration.toLong())

        val bikeTrainingPlan = BikeTrainingPlan(
            planId = planId,
            athleteName = request.athleteData.nome,
            athleteAge = request.athleteData.idade,
            athleteWeight = request.athleteData.peso,
            athleteHeight = request.athleteData.altura,
            experienceLevel = request.athleteData.experiencia,
            trainingGoal = request.athleteData.objetivo,
            diasDisponiveis = request.athleteData.diasDisponiveis,
            volumeSemanalAtual = request.athleteData.volumeSemanalAtual,
            tipoBike = request.athleteData.tipoBike,
            ftpAtual = request.athleteData.ftpAtual,
            potenciaMediaAtual = request.athleteData.potenciaMediaAtual,
            melhorTempo40km = request.athleteData.melhorTempo40km,
            melhorTempo100km = request.athleteData.melhorTempo100km,
            melhorTempo160km = request.athleteData.melhorTempo160km,
            tempoObjetivo = request.athleteData.tempoObjetivo,
            dataProva = request.athleteData.dataProva,
            historicoLesoes = request.athleteData.historicoLesoes,
            experienciaAnterior = request.athleteData.experienciaAnterior,
            preferenciaTreino = request.athleteData.preferenciaTreino,
            equipamentosDisponiveis = request.athleteData.equipamentosDisponiveis,
            zonaTreinoPreferida = request.athleteData.zonaTreinoPreferida,
            planDuration = request.planDuration,
            planContent = "", // Vazio por enquanto
            excelFilePath = "", // Será preenchido quando o plano for gerado
            pdfFilePath = "", // Será preenchido quando o plano for gerado
            status = PlanStatus.PAYMENT_PENDING,
            createdAt = LocalDateTime.now(),
            user = user,
            startDate = startDate,
            endDate = endDate
        )

        bikeTrainingPlanRepository.save(bikeTrainingPlan)

        logger.info("Plano de bike criado com sucesso com ID: $planId para o usuário: $userEmail")

        return BikePlanResponse(
            planId = planId,
            message = "Plano de bike criado com sucesso! Proceed para o pagamento para gerar o conteúdo."
        )
    }

    /**
     * Gera o conteúdo de um plano que já foi aprovado para pagamento
     */
    @Transactional
    fun generateApprovedPlan(planId: String, userEmail: String): BikePlanResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val bikeTrainingPlan = bikeTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        if (bikeTrainingPlan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw IllegalArgumentException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        if (bikeTrainingPlan.status != PlanStatus.PAYMENT_APPROVED) {
            throw IllegalStateException("Plano ainda não foi pago")
        }

        // Marcar como em geração
        bikeTrainingPlan.status = PlanStatus.GENERATING
        bikeTrainingPlanRepository.save(bikeTrainingPlan)

        // Enviar para fila de geração assíncrona
        planGenerationProducer.sendPlanGenerationRequest(planId, userEmail, PlanType.BIKE)

        logger.info("Plano de bike enviado para fila de geração: $planId")

        return BikePlanResponse(
            planId = planId,
            message = "Plano enviado para geração. Você receberá uma notificação quando estiver pronto."
        )
    }

    /**
     * Método legado para manter compatibilidade - cria e gera plano em um passo
     */
    @Transactional
    fun generatePlan(request: BikePlanRequest, userEmail: String): BikePlanResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        val planId = UUID.randomUUID().toString()

        val startDate = if (request.startDate != null && request.startDate != "") {
            LocalDate.parse(request.startDate)
        } else {
            val today = LocalDate.now()
            if (today.dayOfWeek.value == 1) {
                today
            } else {
                today.plusDays((8 - today.dayOfWeek.value).toLong())
            }
        }

        val endDate = startDate.plusWeeks(request.planDuration.toLong())

        val bikeTrainingPlan = BikeTrainingPlan(
            planId = planId,
            athleteName = request.athleteData.nome,
            athleteAge = request.athleteData.idade,
            athleteWeight = request.athleteData.peso,
            athleteHeight = request.athleteData.altura,
            experienceLevel = request.athleteData.experiencia,
            trainingGoal = request.athleteData.objetivo,
            diasDisponiveis = request.athleteData.diasDisponiveis,
            volumeSemanalAtual = request.athleteData.volumeSemanalAtual,
            tipoBike = request.athleteData.tipoBike,
            ftpAtual = request.athleteData.ftpAtual,
            potenciaMediaAtual = request.athleteData.potenciaMediaAtual,
            melhorTempo40km = request.athleteData.melhorTempo40km,
            melhorTempo100km = request.athleteData.melhorTempo100km,
            melhorTempo160km = request.athleteData.melhorTempo160km,
            tempoObjetivo = request.athleteData.tempoObjetivo,
            dataProva = request.athleteData.dataProva,
            historicoLesoes = request.athleteData.historicoLesoes,
            experienciaAnterior = request.athleteData.experienciaAnterior,
            preferenciaTreino = request.athleteData.preferenciaTreino,
            equipamentosDisponiveis = request.athleteData.equipamentosDisponiveis,
            zonaTreinoPreferida = request.athleteData.zonaTreinoPreferida,
            planDuration = request.planDuration,
            planContent = "",
            excelFilePath = "",
            pdfFilePath = "",
            status = PlanStatus.GENERATING,
            createdAt = LocalDateTime.now(),
            user = user,
            startDate = startDate,
            endDate = endDate
        )

        bikeTrainingPlanRepository.save(bikeTrainingPlan)

        planGenerationProducer.sendPlanGenerationRequest(planId, userEmail, PlanType.BIKE)

        logger.info("Plano de bike criado e enviado para geração: $planId")

        return BikePlanResponse(
            planId = planId,
            message = "Plano de bike criado com sucesso! A geração está em andamento."
        )
    }

    fun getPlanExcel(planId: String, userEmail: String): ByteArray {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado") }

        val plan = bikeTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        if (plan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw IllegalArgumentException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        if (plan.status != PlanStatus.COMPLETED) {
            throw IllegalStateException("Plano ainda não foi gerado")
        }

        return fileStorageService.readFile(plan.excelFilePath)
    }

    fun getPlanPdf(planId: String, userEmail: String): ByteArray {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado") }

        val plan = bikeTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        if (plan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw IllegalArgumentException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        if (plan.status != PlanStatus.COMPLETED) {
            throw IllegalStateException("Plano ainda não foi gerado")
        }

        return fileStorageService.readFile(plan.pdfFilePath)
    }

    fun getPlanContent(planId: String, userEmail: String): BikePlanDetailsResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado") }

        val plan = bikeTrainingPlanRepository.findByPlanId(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado com o ID: $planId") }

        // Verificar se o plano pertence ao usuário ou se o usuário é admin
        if (plan.user?.id != user.id && !user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw IllegalArgumentException("Acesso negado. Este plano não pertence ao usuário logado.")
        }

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        return BikePlanDetailsResponse(
            planId = plan.planId,
            athleteName = plan.athleteName,
            athleteAge = plan.athleteAge,
            athleteWeight = plan.athleteWeight,
            athleteHeight = plan.athleteHeight,
            experienceLevel = plan.experienceLevel,
            trainingGoal = plan.trainingGoal,
            diasDisponiveis = plan.diasDisponiveis,
            volumeSemanalAtual = plan.volumeSemanalAtual,
            tipoBike = plan.tipoBike,
            ftpAtual = plan.ftpAtual,
            potenciaMediaAtual = plan.potenciaMediaAtual,
            melhorTempo40km = plan.melhorTempo40km,
            melhorTempo100km = plan.melhorTempo100km,
            melhorTempo160km = plan.melhorTempo160km,
            tempoObjetivo = plan.tempoObjetivo,
            dataProva = plan.dataProva,
            historicoLesoes = plan.historicoLesoes,
            experienciaAnterior = plan.experienciaAnterior,
            preferenciaTreino = plan.preferenciaTreino,
            equipamentosDisponiveis = plan.equipamentosDisponiveis,
            zonaTreinoPreferida = plan.zonaTreinoPreferida,
            planDuration = plan.planDuration,
            planContent = plan.planContent,
            pdfFilePath = plan.pdfFilePath,
            excelFilePath = plan.excelFilePath,
            createdAt = plan.createdAt.format(formatter),
            status = plan.status,
            canGenerate = plan.status == PlanStatus.PAYMENT_PENDING || plan.status == PlanStatus.PAYMENT_APPROVED,
            startDate = plan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            endDate = plan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        )
    }

    fun getUserPlans(userEmail: String): List<BikePlanDetailsResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado") }

        val plans = bikeTrainingPlanRepository.findByUserOrderByCreatedAtDesc(user)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        return plans.map { plan ->
            BikePlanDetailsResponse(
                planId = plan.planId,
                athleteName = plan.athleteName,
                athleteAge = plan.athleteAge,
                athleteWeight = plan.athleteWeight,
                athleteHeight = plan.athleteHeight,
                experienceLevel = plan.experienceLevel,
                trainingGoal = plan.trainingGoal,
                diasDisponiveis = plan.diasDisponiveis,
                volumeSemanalAtual = plan.volumeSemanalAtual,
                tipoBike = plan.tipoBike,
                ftpAtual = plan.ftpAtual,
                potenciaMediaAtual = plan.potenciaMediaAtual,
                melhorTempo40km = plan.melhorTempo40km,
                melhorTempo100km = plan.melhorTempo100km,
                melhorTempo160km = plan.melhorTempo160km,
                tempoObjetivo = plan.tempoObjetivo,
                dataProva = plan.dataProva,
                historicoLesoes = plan.historicoLesoes,
                experienciaAnterior = plan.experienciaAnterior,
                preferenciaTreino = plan.preferenciaTreino,
                equipamentosDisponiveis = plan.equipamentosDisponiveis,
                zonaTreinoPreferida = plan.zonaTreinoPreferida,
                planDuration = plan.planDuration,
                planContent = plan.planContent,
                pdfFilePath = plan.pdfFilePath,
                excelFilePath = plan.excelFilePath,
                createdAt = plan.createdAt.format(formatter),
                status = plan.status,
                canGenerate = plan.status == PlanStatus.PAYMENT_PENDING || plan.status == PlanStatus.PAYMENT_APPROVED,
                startDate = plan.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                endDate = plan.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            )
        }
    }

    @Transactional
    fun processGeneratedPlan(planId: String, planContent: String, excelFilePath: String, pdfFilePath: String) {
        val plan = bikeTrainingPlanRepository.findById(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado: $planId") }

        plan.planContent = planContent
        plan.excelFilePath = excelFilePath
        plan.pdfFilePath = pdfFilePath
        plan.status = PlanStatus.COMPLETED

        bikeTrainingPlanRepository.save(plan)
        logger.info("Plano de bike processado com sucesso: $planId")
    }

    @Transactional
    fun updatePlanStatus(planId: String, status: PlanStatus) {
        val plan = bikeTrainingPlanRepository.findById(planId)
            .orElseThrow { IllegalArgumentException("Plano não encontrado: $planId") }

        plan.status = status
        bikeTrainingPlanRepository.save(plan)
        logger.info("Status do plano de bike atualizado: $planId -> $status")
    }
}

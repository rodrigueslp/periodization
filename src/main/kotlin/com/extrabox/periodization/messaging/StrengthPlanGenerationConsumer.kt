package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.model.StrengthAthleteData
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import com.extrabox.periodization.repository.StrengthTrainingPlanRepository
import com.extrabox.periodization.service.FileStorageService
import com.extrabox.periodization.service.PdfGenerationService
import com.extrabox.periodization.service.StrengthAnthropicService
import com.extrabox.periodization.service.StrengthPdfGenerationService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream

@Component
class StrengthPlanGenerationConsumer(
    private val strengthAnthropicService: StrengthAnthropicService,
    private val strengthTrainingPlanRepository: StrengthTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val strengthPdfGenerationService: StrengthPdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(StrengthPlanGenerationConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.PLAN_GENERATION_QUEUE])
    @Transactional
    fun processPlanGeneration(message: PlanGenerationMessage) {
        // Ignorar mensagens que não são para treinos de musculação
        if (message.planType != PlanType.STRENGTH) {
            return
        }

        logger.info("Recebida mensagem para gerar plano de musculação: ${message.planId}, usuário: ${message.userEmail}")

        try {
            // Buscar o plano pelo ID
            val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(message.planId)
                .orElseThrow { RuntimeException("Plano de musculação não encontrado com o ID: ${message.planId}") }

            // Verificar se o plano está no estado correto para ser gerado
            if (strengthTrainingPlan.status != PlanStatus.QUEUED &&
                strengthTrainingPlan.status != PlanStatus.PAYMENT_APPROVED &&
                strengthTrainingPlan.status != PlanStatus.FAILED) {
                logger.warn("Plano de musculação ${message.planId} não está em estado adequado para geração. Status atual: ${strengthTrainingPlan.status}")
                return
            }

            // Atualizar status para GENERATING
            strengthTrainingPlan.status = PlanStatus.GENERATING
            strengthTrainingPlanRepository.save(strengthTrainingPlan)
            logger.info("Status do plano de musculação ${message.planId} atualizado para GENERATING")

            // Construir o objeto StrengthAthleteData a partir dos dados do plano
            val athleteData = StrengthAthleteData(
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
                periodoTreino = null,
                trainingFocus = strengthTrainingPlan.trainingFocus,
                equipmentAvailable = strengthTrainingPlan.equipmentAvailable,
                sessionsPerWeek = strengthTrainingPlan.sessionsPerWeek,
                sessionDuration = strengthTrainingPlan.sessionDuration
            )

            logger.info("Iniciando geração de conteúdo para plano de musculação ${message.planId}")
            // Gerar o conteúdo do plano usando o serviço Anthropic
            val planContent = strengthAnthropicService.generateStrengthTrainingPlan(athleteData, strengthTrainingPlan.planDuration)
            logger.info("Conteúdo gerado com sucesso para plano de musculação ${message.planId}")

            // Criar planilha Excel
//            val excelData = createExcelWorkbook(athleteData, planContent)
//            val excelFilePath = fileStorageService.saveFile(message.planId, excelData)

            // Criar arquivo PDF
            val pdfData = strengthPdfGenerationService.generatePdf(athleteData, planContent)
            val pdfFilePath = fileStorageService.savePdfFile(message.planId, pdfData)

            // Atualizar o plano com o conteúdo gerado
            strengthTrainingPlan.planContent = planContent
            strengthTrainingPlan.excelFilePath = ""
            strengthTrainingPlan.pdfFilePath = pdfFilePath
            strengthTrainingPlan.status = PlanStatus.COMPLETED
            strengthTrainingPlanRepository.save(strengthTrainingPlan)

            logger.info("Plano de musculação ${message.planId} gerado com sucesso e marcado como COMPLETED")
        } catch (e: Exception) {
            logger.error("Erro ao gerar plano de musculação ${message.planId}: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            try {
                val strengthTrainingPlan = strengthTrainingPlanRepository.findByPlanId(message.planId).orElse(null)
                if (strengthTrainingPlan != null) {
                    strengthTrainingPlan.status = PlanStatus.FAILED
                    strengthTrainingPlanRepository.save(strengthTrainingPlan)
                    logger.info("Plano de musculação ${message.planId} marcado como FAILED devido a erro na geração")
                }
            } catch (ex: Exception) {
                logger.error("Erro ao atualizar status do plano de musculação para FAILED: ${ex.message}", ex)
            }
        }
    }

}

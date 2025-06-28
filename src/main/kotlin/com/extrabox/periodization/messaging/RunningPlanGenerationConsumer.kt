package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.model.RunningAthleteData
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import com.extrabox.periodization.repository.RunningTrainingPlanRepository
import com.extrabox.periodization.service.FileStorageService
import com.extrabox.periodization.service.RunningAnthropicService
import com.extrabox.periodization.service.RunningPdfGenerationService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RunningPlanGenerationConsumer(
    private val runningAnthropicService: RunningAnthropicService,
    private val runningTrainingPlanRepository: RunningTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val runningPdfGenerationService: RunningPdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(RunningPlanGenerationConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.PLAN_GENERATION_RUNNING_QUEUE])
    @Transactional
    fun processPlanGeneration(message: PlanGenerationMessage) {
        // Ignorar mensagens que não são para treinos de corrida
        if (message.planType != PlanType.RUNNING) {
            return
        }

        logger.info("Recebida mensagem para gerar plano de corrida: ${message.planId}, usuário: ${message.userEmail}")

        try {
            // Buscar o plano pelo ID
            val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(message.planId)
                .orElseThrow { RuntimeException("Plano de corrida não encontrado com o ID: ${message.planId}") }

            // Verificar se o plano está no estado correto para ser gerado
            if (runningTrainingPlan.status != PlanStatus.QUEUED &&
                runningTrainingPlan.status != PlanStatus.PAYMENT_APPROVED &&
                runningTrainingPlan.status != PlanStatus.FAILED) {
                logger.warn("Plano de corrida ${message.planId} não está em estado adequado para geração. Status atual: ${runningTrainingPlan.status}")
                return
            }

            // Atualizar status para GENERATING
            runningTrainingPlan.status = PlanStatus.GENERATING
            runningTrainingPlanRepository.save(runningTrainingPlan)
            logger.info("Status do plano de corrida ${message.planId} atualizado para GENERATING")

            // Construir o objeto RunningAthleteData a partir dos dados do plano
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

            logger.info("Iniciando geração de conteúdo para plano de corrida ${message.planId}")
            // Gerar o conteúdo do plano usando o serviço Anthropic
            val planContent = runningAnthropicService.generateRunningTrainingPlan(athleteData, runningTrainingPlan.planDuration)
            logger.info("Conteúdo gerado com sucesso para plano de corrida ${message.planId}")

            // Criar planilha Excel (comentado por enquanto, similar aos outros serviços)
            // val excelData = createExcelWorkbook(athleteData, planContent)
            // val excelFilePath = fileStorageService.saveFile(message.planId, excelData)

            // Criar arquivo PDF
            val pdfData = runningPdfGenerationService.generatePdf(athleteData, planContent)
            val pdfFilePath = fileStorageService.savePdfFile(message.planId, pdfData)

            // Atualizar o plano com o conteúdo gerado
            runningTrainingPlan.planContent = planContent
            runningTrainingPlan.excelFilePath = ""
            runningTrainingPlan.pdfFilePath = pdfFilePath
            runningTrainingPlan.status = PlanStatus.COMPLETED
            runningTrainingPlanRepository.save(runningTrainingPlan)

            logger.info("Plano de corrida ${message.planId} gerado com sucesso e marcado como COMPLETED")
        } catch (e: Exception) {
            logger.error("Erro ao gerar plano de corrida ${message.planId}: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            try {
                val runningTrainingPlan = runningTrainingPlanRepository.findByPlanId(message.planId).orElse(null)
                if (runningTrainingPlan != null) {
                    runningTrainingPlan.status = PlanStatus.FAILED
                    runningTrainingPlanRepository.save(runningTrainingPlan)
                    logger.info("Plano de corrida ${message.planId} marcado como FAILED devido a erro na geração")
                }
            } catch (ex: Exception) {
                logger.error("Erro ao atualizar status do plano de corrida para FAILED: ${ex.message}", ex)
            }
        }
    }
}

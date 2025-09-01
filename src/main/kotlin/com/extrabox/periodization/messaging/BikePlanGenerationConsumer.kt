package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.model.BikeAthleteData
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import com.extrabox.periodization.repository.BikeTrainingPlanRepository
import com.extrabox.periodization.service.FileStorageService
import com.extrabox.periodization.service.BikeAnthropicService
import com.extrabox.periodization.service.BikePdfGenerationService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BikePlanGenerationConsumer(
    private val bikeAnthropicService: BikeAnthropicService,
    private val bikeTrainingPlanRepository: BikeTrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val bikePdfGenerationService: BikePdfGenerationService
) {
    private val logger = LoggerFactory.getLogger(BikePlanGenerationConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.PLAN_GENERATION_BIKE_QUEUE])
    @Transactional
    fun processPlanGeneration(message: PlanGenerationMessage) {
        // Ignorar mensagens que não são para treinos de bike
        if (message.planType != PlanType.BIKE) {
            logger.warn("Mensagem ignorada - tipo incorreto: ${message.planType}")
            return
        }

        logger.info("Processando geração de plano de bike: ${message.planId}")

        try {
            val plan = bikeTrainingPlanRepository.findById(message.planId)
                .orElseThrow { IllegalArgumentException("Plano não encontrado: ${message.planId}") }

            // Verificar se o plano está no status correto
            if (plan.status != PlanStatus.GENERATING) {
                logger.warn("Plano ${message.planId} não está em status GENERATING. Status atual: ${plan.status}")
                return
            }

            // Criar objeto de dados do atleta
            val athleteData = BikeAthleteData(
                nome = plan.athleteName,
                idade = plan.athleteAge,
                peso = plan.athleteWeight,
                altura = plan.athleteHeight,
                experiencia = plan.experienceLevel,
                objetivo = plan.trainingGoal,
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
                zonaTreinoPreferida = plan.zonaTreinoPreferida
            )

            // Gerar o conteúdo do plano
            logger.info("Gerando conteúdo para plano de bike: ${message.planId}")
            val planContent = bikeAnthropicService.generateBikeTrainingPlan(athleteData, plan.planDuration)

            // Gerar PDF
            logger.info("Gerando PDF para plano de bike: ${message.planId}")
            val pdfBytes = bikePdfGenerationService.generatePdf(athleteData, planContent)
            val pdfFileName = "bike_plan_${message.planId}.pdf"
            val pdfFilePath = fileStorageService.saveFileWithName(pdfBytes, pdfFileName)

            // Gerar Excel (usando um serviço genérico ou específico se existir)
            logger.info("Gerando Excel para plano de bike: ${message.planId}")
            val excelBytes = generateExcelContent(planContent, athleteData)
            val excelFileName = "bike_plan_${message.planId}.xlsx"
            val excelFilePath = fileStorageService.saveFileWithName(excelBytes, excelFileName)

            // Atualizar o plano com o conteúdo gerado
            plan.planContent = planContent
            plan.pdfFilePath = pdfFilePath
            plan.excelFilePath = excelFilePath
            plan.status = PlanStatus.COMPLETED

            bikeTrainingPlanRepository.save(plan)

            logger.info("Plano de bike gerado com sucesso: ${message.planId}")

        } catch (e: Exception) {
            logger.error("Erro ao processar plano de bike ${message.planId}", e)
            
            // Marcar plano como erro
            try {
                val plan = bikeTrainingPlanRepository.findById(message.planId).orElse(null)
                plan?.let {
                    it.status = PlanStatus.FAILED
                    bikeTrainingPlanRepository.save(it)
                }
            } catch (saveError: Exception) {
                logger.error("Erro ao marcar plano como falho: ${message.planId}", saveError)
            }
        }
    }

    private fun generateExcelContent(planContent: String, athleteData: BikeAthleteData): ByteArray {
        // Por enquanto, vamos retornar um Excel simples
        // Futuramente pode ser implementado um serviço específico para Excel
        val content = """
            PLANO DE TREINO DE BIKE
            
            Atleta: ${athleteData.nome}
            Idade: ${athleteData.idade} anos
            Objetivo: ${athleteData.objetivo}
            
            $planContent
        """.trimIndent()
        
        return content.toByteArray()
    }
}

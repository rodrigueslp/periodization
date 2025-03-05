package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.enums.PlanStatus
import com.extrabox.periodization.model.AthleteData
import com.extrabox.periodization.model.Benchmarks
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import com.extrabox.periodization.repository.BenchmarkDataRepository
import com.extrabox.periodization.repository.TrainingPlanRepository
import com.extrabox.periodization.service.AnthropicService
import com.extrabox.periodization.service.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook

@Component
class PlanGenerationConsumer(
    private val anthropicService: AnthropicService,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val benchmarkDataRepository: BenchmarkDataRepository,
    private val fileStorageService: FileStorageService
) {
    private val logger = LoggerFactory.getLogger(PlanGenerationConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.PLAN_GENERATION_QUEUE])
    @Transactional
    fun processPlanGeneration(message: PlanGenerationMessage) {
        logger.info("Recebida mensagem para gerar plano: ${message.planId}, usuário: ${message.userEmail}")

        try {
            // Buscar o plano pelo ID
            val trainingPlan = trainingPlanRepository.findByPlanId(message.planId)
                .orElseThrow { RuntimeException("Plano não encontrado com o ID: ${message.planId}") }

            // Verificar se o plano está no estado correto para ser gerado
            if (trainingPlan.status != PlanStatus.QUEUED &&
                trainingPlan.status != PlanStatus.PAYMENT_APPROVED &&
                trainingPlan.status != PlanStatus.FAILED) {
                logger.warn("Plano ${message.planId} não está em estado adequado para geração. Status atual: ${trainingPlan.status}")
                return
            }

            // Atualizar status para GENERATING
            trainingPlan.status = PlanStatus.GENERATING
            trainingPlanRepository.save(trainingPlan)
            logger.info("Status do plano ${message.planId} atualizado para GENERATING")

            // Construir o objeto AthleteData a partir dos dados do plano
            val athleteData = AthleteData(
                nome = trainingPlan.athleteName,
                idade = trainingPlan.athleteAge,
                peso = trainingPlan.athleteWeight,
                altura = trainingPlan.athleteHeight,
                experiencia = trainingPlan.experienceLevel,
                objetivo = trainingPlan.trainingGoal,
                disponibilidade = trainingPlan.availability,
                lesoes = trainingPlan.injuries,
                historico = trainingPlan.trainingHistory,
                benchmarks = benchmarkDataRepository.findByPlanId(message.planId).orElse(null)?.let {
                    Benchmarks(
                        backSquat = it.backSquat,
                        deadlift = it.deadlift,
                        clean = it.clean,
                        snatch = it.snatch,
                        fran = it.fran,
                        grace = it.grace
                    )
                }
            )

            logger.info("Iniciando geração de conteúdo para plano ${message.planId}")
            // Gerar o conteúdo do plano usando o serviço Anthropic
            val planContent = anthropicService.generateTrainingPlan(athleteData, trainingPlan.planDuration)
            logger.info("Conteúdo gerado com sucesso para plano ${message.planId}")

            // Criar planilha Excel
            logger.info("Criando planilha Excel para plano ${message.planId}")
            val excelData = createExcelWorkbook(athleteData, planContent)
            val excelFilePath = fileStorageService.saveFile(message.planId, excelData)
            logger.info("Planilha Excel criada e salva com sucesso: $excelFilePath")

            // Atualizar o plano com o conteúdo gerado
            trainingPlan.planContent = planContent
            trainingPlan.excelFilePath = excelFilePath
            trainingPlan.status = PlanStatus.COMPLETED
            trainingPlanRepository.save(trainingPlan)

            logger.info("Plano ${message.planId} gerado com sucesso e marcado como COMPLETED")
        } catch (e: Exception) {
            logger.error("Erro ao gerar plano ${message.planId}: ${e.message}", e)

            // Em caso de erro, marcar o plano como FAILED
            try {
                val trainingPlan = trainingPlanRepository.findByPlanId(message.planId).orElse(null)
                if (trainingPlan != null) {
                    trainingPlan.status = PlanStatus.FAILED
                    trainingPlanRepository.save(trainingPlan)
                    logger.info("Plano ${message.planId} marcado como FAILED devido a erro na geração")
                }
            } catch (ex: Exception) {
                logger.error("Erro ao atualizar status do plano para FAILED: ${ex.message}", ex)
            }
        }
    }

    private fun createExcelWorkbook(athleteData: AthleteData, planContent: String): ByteArray {
        val workbook = XSSFWorkbook()

        // Criar folha de informações do atleta
        val infoSheet = workbook.createSheet("Informações do Atleta")

        var rowIndex = 0

        // Título
        var row = infoSheet.createRow(rowIndex++)
        var cell = row.createCell(0)
        cell.setCellValue("PLANO DE PERIODIZAÇÃO DE CROSSFIT")

        // Espaço
        rowIndex++

        // Informações do atleta
        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Nome:")
        row.createCell(1).setCellValue(athleteData.nome)

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Idade:")
        row.createCell(1).setCellValue(athleteData.idade.toString() + " anos")

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Peso:")
        row.createCell(1).setCellValue(athleteData.peso.toString() + " kg")

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Altura:")
        row.createCell(1).setCellValue(athleteData.altura.toString() + " cm")

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Experiência:")
        row.createCell(1).setCellValue(athleteData.experiencia)

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Objetivo:")
        row.createCell(1).setCellValue(athleteData.objetivo)

        row = infoSheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("Disponibilidade:")
        row.createCell(1).setCellValue(athleteData.disponibilidade.toString() + " dias por semana")

        // Criar folha de plano de treinamento
        val planSheet = workbook.createSheet("Plano de Treinamento")

        rowIndex = 0
        row = planSheet.createRow(rowIndex++)
        cell = row.createCell(0)
        cell.setCellValue("PLANO DE TREINAMENTO")

        // Adicionar o conteúdo do plano
        val lines = planContent.split("\n")
        for (line in lines) {
            row = planSheet.createRow(rowIndex++)
            cell = row.createCell(0)
            cell.setCellValue(line)
        }

        // Ajustar largura das colunas
        infoSheet.autoSizeColumn(0)
        infoSheet.autoSizeColumn(1)
        planSheet.autoSizeColumn(0)

        // Converter o workbook para ByteArray
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream.toByteArray()
    }
}
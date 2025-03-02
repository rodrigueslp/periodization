package com.extrabox.periodization.service

import com.extrabox.periodization.entity.BenchmarkData
import com.extrabox.periodization.entity.TrainingPlan
import com.extrabox.periodization.model.AthleteData
import com.extrabox.periodization.model.PlanDetailsResponse
import com.extrabox.periodization.model.PlanRequest
import com.extrabox.periodization.model.PlanResponse
import com.extrabox.periodization.repository.BenchmarkDataRepository
import com.extrabox.periodization.repository.TrainingPlanRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.*
import java.time.format.DateTimeFormatter

@Service
class PeriodizationService(
    private val anthropicService: AnthropicService,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val benchmarkDataRepository: BenchmarkDataRepository,
    private val fileStorageService: FileStorageService
) {

    @Transactional
    fun generatePlan(request: PlanRequest): PlanResponse {
        val planId = UUID.randomUUID().toString()
        val planContent = anthropicService.generateTrainingPlan(request.athleteData, request.planDuration)

        // Criar planilha Excel a partir do conteúdo do plano
        val excelData = createExcelWorkbook(request.athleteData, planContent)

        // Salvar o arquivo Excel e obter o caminho
        val excelFilePath = fileStorageService.saveFile(planId, excelData)

        // Persistir no banco de dados
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
            planDuration = request.planDuration,
            planContent = planContent,
            excelFilePath = excelFilePath
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
            message = "Plano de treinamento gerado com sucesso"
        )
    }

    fun getPlanExcel(planId: String): ByteArray {
        val trainingPlan = trainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }
        return fileStorageService.loadFile(planId)
    }

    fun getPlanContent(planId: String): PlanDetailsResponse {
        val trainingPlan = trainingPlanRepository.findByPlanId(planId)
            .orElseThrow { RuntimeException("Plano não encontrado com o ID: $planId") }

        val benchmarks = benchmarkDataRepository.findByPlanId(planId).orElse(null)

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
            createdAt = trainingPlan.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
            benchmarks = benchmarks?.let {
                mapOf(
                    "backSquat" to it.backSquat,
                    "deadlift" to it.deadlift,
                    "clean" to it.clean,
                    "snatch" to it.snatch,
                    "fran" to it.fran,
                    "grace" to it.grace
                )
            }
        )
    }

    fun getAllPlans(): List<PlanDetailsResponse> {
        return trainingPlanRepository.findTop10ByOrderByCreatedAtDesc().map { plan ->
            val benchmarks = benchmarkDataRepository.findByPlanId(plan.planId).orElse(null)

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
                createdAt = plan.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                benchmarks = benchmarks?.let {
                    mapOf(
                        "backSquat" to it.backSquat,
                        "deadlift" to it.deadlift,
                        "clean" to it.clean,
                        "snatch" to it.snatch,
                        "fran" to it.fran,
                        "grace" to it.grace
                    )
                }
            )
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

package com.extrabox.periodization.controller

import com.extrabox.periodization.model.PlanDetailsResponse
import com.extrabox.periodization.model.PlanRequest
import com.extrabox.periodization.model.PlanResponse
import com.extrabox.periodization.service.PeriodizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/periodization")
@Tag(name = "Periodização", description = "Endpoints para gestão de planos de periodização")
class PeriodizationController(
    private val periodizationService: PeriodizationService
) {

    @PostMapping
    @Operation(summary = "Gerar um novo plano de periodização", description = "Cria um plano de periodização baseado nos dados do atleta")
    fun generatePlan(@Valid @RequestBody request: PlanRequest): ResponseEntity<PlanResponse> {
        val response = periodizationService.generatePlan(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{planId}/download")
    @Operation(summary = "Baixar planilha de periodização", description = "Baixar o plano de periodização no formato Excel")
    fun downloadPlan(@PathVariable planId: String): ResponseEntity<ByteArray> {
        val excelData = periodizationService.getPlanExcel(planId)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "periodizacao_crossfit_$planId.xlsx")

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData)
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Obter detalhes do plano", description = "Retorna os detalhes do plano em formato texto")
    fun getPlanDetails(@PathVariable planId: String): ResponseEntity<PlanDetailsResponse> {
        val planDetails = periodizationService.getPlanContent(planId)
        return ResponseEntity.ok(planDetails)
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Retorna os últimos planos gerados")
    fun getAllPlans(): ResponseEntity<List<PlanDetailsResponse>> {
        val plans = periodizationService.getAllPlans()
        return ResponseEntity.ok(plans)
    }
}

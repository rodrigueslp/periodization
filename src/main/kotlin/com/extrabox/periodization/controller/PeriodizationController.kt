package com.extrabox.periodization.controller

import com.extrabox.periodization.model.PlanDetailsResponse
import com.extrabox.periodization.model.PlanRequest
import com.extrabox.periodization.model.PlanResponse
import com.extrabox.periodization.service.PeriodizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/periodization")
@Tag(name = "Periodização", description = "Endpoints para gestão de planos de periodização")
@SecurityRequirement(name = "bearerAuth")
class PeriodizationController(
    private val periodizationService: PeriodizationService
) {

    @PostMapping
    @Operation(summary = "Criar um plano pendente de pagamento", description = "Cria um plano pendente de pagamento baseado nos dados do atleta")
    fun createPendingPlan(
        @Valid @RequestBody request: PlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PlanResponse> {
        val response = periodizationService.createPendingPlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{planId}/generate")
    @Operation(summary = "Gerar conteúdo de um plano aprovado", description = "Gera o conteúdo de um plano que já foi aprovado para pagamento")
    fun generateApprovedPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PlanResponse> {
        val response = periodizationService.generateApprovedPlan(planId, userDetails.username)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/legacy")
    @Operation(summary = "Gerar um novo plano (legado)", description = "Método legado para compatibilidade - cria e gera um plano em um único passo")
    fun generatePlan(
        @Valid @RequestBody request: PlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PlanResponse> {
        val response = periodizationService.generatePlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{planId}/download")
    @Operation(summary = "Baixar planilha de periodização", description = "Baixar o plano de periodização no formato Excel")
    fun downloadPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val excelData = periodizationService.getPlanExcel(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "periodizacao_crossfit_$planId.xlsx")

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData)
    }

    @GetMapping("/{planId}/download-pdf")
    @Operation(summary = "Baixar PDF de periodização", description = "Baixar o plano de periodização no formato PDF")
    fun downloadPlanPdf(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val pdfData = periodizationService.getPlanPdf(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "periodizacao_crossfit_$planId.pdf")

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfData)
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Obter detalhes do plano", description = "Retorna os detalhes do plano em formato texto")
    fun getPlanDetails(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PlanDetailsResponse> {
        val planDetails = periodizationService.getPlanContent(planId, userDetails.username)
        return ResponseEntity.ok(planDetails)
    }

    @GetMapping
    @Operation(summary = "Listar planos do usuário", description = "Retorna os planos do usuário logado")
    fun getUserPlans(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<PlanDetailsResponse>> {
        val plans = periodizationService.getUserPlans(userDetails.username)
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os planos", description = "Retorna todos os planos (apenas para administradores)")
    fun getAllPlans(): ResponseEntity<List<PlanDetailsResponse>> {
        val plans = periodizationService.getUserPlans("admin") // O método já verifica se é admin
        return ResponseEntity.ok(plans)
    }
}

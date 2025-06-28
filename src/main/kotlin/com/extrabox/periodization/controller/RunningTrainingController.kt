package com.extrabox.periodization.controller

import com.extrabox.periodization.model.RunningPlanDetailsResponse
import com.extrabox.periodization.model.RunningPlanRequest
import com.extrabox.periodization.model.RunningPlanResponse
import com.extrabox.periodization.service.RunningTrainingService
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
@RequestMapping("/api/running-training")
@Tag(name = "Corrida", description = "Endpoints para gestão de planos de treinamento de corrida")
@SecurityRequirement(name = "bearerAuth")
class RunningTrainingController(
    private val runningTrainingService: RunningTrainingService
) {

    @PostMapping
    @Operation(summary = "Criar um plano de corrida pendente de pagamento", description = "Cria um plano pendente de pagamento baseado nos dados do corredor")
    fun createPendingPlan(
        @Valid @RequestBody request: RunningPlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<RunningPlanResponse> {
        val response = runningTrainingService.createPendingPlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{planId}/generate")
    @Operation(summary = "Gerar conteúdo de um plano de corrida aprovado", description = "Gera o conteúdo de um plano que já foi aprovado para pagamento")
    fun generateApprovedPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<RunningPlanResponse> {
        val response = runningTrainingService.generateApprovedPlan(planId, userDetails.username)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/legacy")
    @Operation(summary = "Gerar um novo plano de corrida (legado)", description = "Método legado para compatibilidade - cria e gera um plano em um único passo")
    fun generatePlan(
        @Valid @RequestBody request: RunningPlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<RunningPlanResponse> {
        val response = runningTrainingService.generatePlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{planId}/download")
    @Operation(summary = "Baixar planilha de corrida", description = "Baixar o plano de corrida no formato Excel")
    fun downloadPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val excelData = runningTrainingService.getPlanExcel(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "corrida_$planId.xlsx")

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData)
    }

    @GetMapping("/{planId}/download-pdf")
    @Operation(summary = "Baixar PDF de corrida", description = "Baixar o plano de corrida no formato PDF")
    fun downloadPlanPdf(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val pdfData = runningTrainingService.getPlanPdf(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "corrida_$planId.pdf")

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfData)
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Obter detalhes do plano de corrida", description = "Retorna os detalhes do plano em formato texto")
    fun getPlanDetails(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<RunningPlanDetailsResponse> {
        val planDetails = runningTrainingService.getPlanContent(planId, userDetails.username)
        return ResponseEntity.ok(planDetails)
    }

    @GetMapping
    @Operation(summary = "Listar planos de corrida do usuário", description = "Retorna os planos do usuário logado")
    fun getUserPlans(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<RunningPlanDetailsResponse>> {
        val plans = runningTrainingService.getUserPlans(userDetails.username)
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os planos de corrida", description = "Retorna todos os planos (apenas para administradores)")
    fun getAllPlans(): ResponseEntity<List<RunningPlanDetailsResponse>> {
        val plans = runningTrainingService.getUserPlans("admin") // O método já verifica se é admin
        return ResponseEntity.ok(plans)
    }
}

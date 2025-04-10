package com.extrabox.periodization.controller

import com.extrabox.periodization.model.StrengthPlanDetailsResponse
import com.extrabox.periodization.model.StrengthPlanRequest
import com.extrabox.periodization.model.StrengthPlanResponse
import com.extrabox.periodization.service.StrengthTrainingService
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
@RequestMapping("/api/strength-training")
@Tag(name = "Musculação", description = "Endpoints para gestão de planos de musculação")
@SecurityRequirement(name = "bearerAuth")
class StrengthTrainingController(
    private val strengthTrainingService: StrengthTrainingService
) {

    @PostMapping
    @Operation(summary = "Criar um plano de musculação pendente de pagamento", description = "Cria um plano pendente de pagamento baseado nos dados do atleta")
    fun createPendingPlan(
        @Valid @RequestBody request: StrengthPlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<StrengthPlanResponse> {
        val response = strengthTrainingService.createPendingPlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{planId}/generate")
    @Operation(summary = "Gerar conteúdo de um plano de musculação aprovado", description = "Gera o conteúdo de um plano que já foi aprovado para pagamento")
    fun generateApprovedPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<StrengthPlanResponse> {
        val response = strengthTrainingService.generateApprovedPlan(planId, userDetails.username)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/legacy")
    @Operation(summary = "Gerar um novo plano de musculação (legado)", description = "Método legado para compatibilidade - cria e gera um plano em um único passo")
    fun generatePlan(
        @Valid @RequestBody request: StrengthPlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<StrengthPlanResponse> {
        val response = strengthTrainingService.generatePlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{planId}/download")
    @Operation(summary = "Baixar planilha de musculação", description = "Baixar o plano de musculação no formato Excel")
    fun downloadPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val excelData = strengthTrainingService.getPlanExcel(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "musculacao_$planId.xlsx")

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData)
    }

    @GetMapping("/{planId}/download-pdf")
    @Operation(summary = "Baixar PDF de musculação", description = "Baixar o plano de musculação no formato PDF")
    fun downloadPlanPdf(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val pdfData = strengthTrainingService.getPlanPdf(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "musculacao_$planId.pdf")

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfData)
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Obter detalhes do plano de musculação", description = "Retorna os detalhes do plano em formato texto")
    fun getPlanDetails(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<StrengthPlanDetailsResponse> {
        val planDetails = strengthTrainingService.getPlanContent(planId, userDetails.username)
        return ResponseEntity.ok(planDetails)
    }

    @GetMapping
    @Operation(summary = "Listar planos de musculação do usuário", description = "Retorna os planos do usuário logado")
    fun getUserPlans(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<StrengthPlanDetailsResponse>> {
        val plans = strengthTrainingService.getUserPlans(userDetails.username)
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os planos de musculação", description = "Retorna todos os planos (apenas para administradores)")
    fun getAllPlans(): ResponseEntity<List<StrengthPlanDetailsResponse>> {
        val plans = strengthTrainingService.getUserPlans("admin") // O método já verifica se é admin
        return ResponseEntity.ok(plans)
    }
}

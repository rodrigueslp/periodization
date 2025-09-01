package com.extrabox.periodization.controller

import com.extrabox.periodization.model.BikePlanDetailsResponse
import com.extrabox.periodization.model.BikePlanRequest
import com.extrabox.periodization.model.BikePlanResponse
import com.extrabox.periodization.service.BikeTrainingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bike-training")
@Tag(name = "Treino de Bike", description = "Endpoints para gestão de planos de treino de bike")
@SecurityRequirement(name = "bearerAuth")
class BikeTrainingController(
    private val bikeTrainingService: BikeTrainingService
) {

    private val logger = LoggerFactory.getLogger(BikeTrainingController::class.java)

    @PostMapping
    @Operation(summary = "Criar um plano de bike pendente de pagamento", description = "Cria um plano de bike pendente de pagamento baseado nos dados do atleta")
    fun createPendingPlan(
        @Valid @RequestBody request: BikePlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<BikePlanResponse> {
        logger.info("[CONTROLLER] Entered createPendingBikePlan()")
        val response = bikeTrainingService.createPendingPlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{planId}/generate")
    @Operation(summary = "Gerar conteúdo de um plano de bike aprovado", description = "Gera o conteúdo de um plano de bike que já foi aprovado para pagamento")
    fun generateApprovedPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<BikePlanResponse> {
        logger.info("[CONTROLLER] Entered generateApprovedBikePlan()")
        val response = bikeTrainingService.generateApprovedPlan(planId, userDetails.username)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/legacy")
    @Operation(summary = "Gerar um novo plano de bike (legado)", description = "Método legado para compatibilidade - cria e gera um plano de bike em um único passo")
    fun generatePlan(
        @Valid @RequestBody request: BikePlanRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<BikePlanResponse> {
        logger.info("[CONTROLLER] Entered generateBikePlan()")
        val response = bikeTrainingService.generatePlan(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{planId}/download")
    @Operation(summary = "Baixar planilha de treino de bike", description = "Baixar o plano de treino de bike no formato Excel")
    fun downloadPlan(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        logger.info("[CONTROLLER] Entered downloadBikePlan()")
        val excelData = bikeTrainingService.getPlanExcel(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "treino_bike_$planId.xlsx")

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData)
    }

    @GetMapping("/{planId}/download-pdf")
    @Operation(summary = "Baixar PDF de treino de bike", description = "Baixar o plano de treino de bike no formato PDF")
    fun downloadPlanPdf(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        logger.info("[CONTROLLER] Entered downloadBikePlanPdf()")
        val pdfData = bikeTrainingService.getPlanPdf(planId, userDetails.username)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "treino_bike_$planId.pdf")

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfData)
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Obter detalhes do plano de bike", description = "Retorna os detalhes do plano de bike em formato texto")
    fun getPlanDetails(
        @PathVariable planId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<BikePlanDetailsResponse> {
        logger.info("[CONTROLLER] Entered getBikePlanDetails()")
        val planDetails = bikeTrainingService.getPlanContent(planId, userDetails.username)
        return ResponseEntity.ok(planDetails)
    }

    @GetMapping
    @Operation(summary = "Listar planos de bike do usuário", description = "Retorna os planos de bike do usuário logado")
    fun getUserPlans(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<BikePlanDetailsResponse>> {
        val plans = bikeTrainingService.getUserPlans(userDetails.username)
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os planos de bike", description = "Retorna todos os planos de bike (apenas para administradores)")
    fun getAllPlans(): ResponseEntity<List<BikePlanDetailsResponse>> {
        val plans = bikeTrainingService.getUserPlans("admin") // O método já verifica se é admin
        return ResponseEntity.ok(plans)
    }
}

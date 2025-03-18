package com.extrabox.periodization.controller

import com.extrabox.periodization.model.FeedbackRequest
import com.extrabox.periodization.model.FeedbackResponse
import com.extrabox.periodization.service.FeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "Endpoints para gestão de feedback dos usuários")
@SecurityRequirement(name = "bearerAuth")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    @PostMapping
    @Operation(summary = "Enviar feedback", description = "Envia um feedback do usuário para o sistema")
    fun submitFeedback(
        @Valid @RequestBody request: FeedbackRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<FeedbackResponse> {
        val response = feedbackService.saveFeedback(request, userDetails.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/user")
    @Operation(summary = "Obter feedbacks do usuário", description = "Retorna todos os feedbacks enviados pelo usuário")
    fun getUserFeedbacks(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<FeedbackResponse>> {
        val feedbacks = feedbackService.getUserFeedbacks(userDetails.username)
        return ResponseEntity.ok(feedbacks)
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obter todos os feedbacks", description = "Retorna todos os feedbacks dos usuários (apenas para administradores)")
    @SecurityRequirement(name = "bearerAuth")
    fun getAllFeedbacks(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<FeedbackResponse>> {
        val feedbacks = feedbackService.getAllFeedbacks(userDetails.username, type)
        return ResponseEntity.ok(feedbacks)
    }
}

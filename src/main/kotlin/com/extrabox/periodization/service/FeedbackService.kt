package com.extrabox.periodization.service

import com.extrabox.periodization.entity.Role
import com.extrabox.periodization.entity.UserFeedback
import com.extrabox.periodization.model.FeedbackRequest
import com.extrabox.periodization.model.FeedbackResponse
import com.extrabox.periodization.repository.UserFeedbackRepository
import com.extrabox.periodization.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FeedbackService(
    private val userFeedbackRepository: UserFeedbackRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun saveFeedback(request: FeedbackRequest, userEmail: String): FeedbackResponse {
        // Obter usuário pelo email
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Criar nova entidade de feedback
        val feedback = UserFeedback(
            feedbackText = request.feedbackText,
            feedbackType = request.feedbackType,
            user = user,
            planId = request.planId
        )

        // Salvar o feedback
        val savedFeedback = userFeedbackRepository.save(feedback)

        // Retornar resposta
        return FeedbackResponse(
            id = savedFeedback.id!!,
            feedbackText = savedFeedback.feedbackText,
            feedbackType = savedFeedback.feedbackType,
            createdAt = savedFeedback.createdAt.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withLocale(Locale.of("pt", "BR"))
            ),
            userName = user.fullName,
            userEmail = user.email
        )
    }

    fun getUserFeedbacks(userEmail: String): List<FeedbackResponse> {
        // Obter usuário pelo email
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        // Buscar feedbacks do usuário
        val feedbacks = userFeedbackRepository.findByUserOrderByCreatedAtDesc(user)

        // Mapear para resposta
        return feedbacks.map { feedback ->
            FeedbackResponse(
                id = feedback.id!!,
                feedbackText = feedback.feedbackText,
                feedbackType = feedback.feedbackType,
                createdAt = feedback.createdAt.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        .withLocale(Locale.of("pt", "BR"))
                ),
                userName = user.fullName,
                userEmail = user.email,
                planId = feedback.planId
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAllFeedbacks(userEmail: String, type: String?): List<FeedbackResponse> {
        // Verificar se o usuário é admin
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado com o email: $userEmail") }

        if (!user.roles.any { it.name == Role.ROLE_ADMIN }) {
            throw AccessDeniedException("Apenas administradores podem acessar todos os feedbacks")
        }

        // Buscar feedbacks, filtrados por tipo se especificado
        val feedbacks = if (type != null) {
            userFeedbackRepository.findByFeedbackTypeOrderByCreatedAtDesc(type)
        } else {
            userFeedbackRepository.findAllByOrderByCreatedAtDesc()
        }

        // Converter para FeedbackResponse
        return feedbacks.map { feedback ->
            FeedbackResponse(
                id = feedback.id ?: 0,
                feedbackText = feedback.feedbackText,
                feedbackType = feedback.feedbackType,
                createdAt = feedback.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                userName = feedback.user?.fullName ?: "Anônimo",
                userEmail = feedback.user?.email ?: "anônimo@email.com",
                planId = feedback.planId
            )
        }
    }
}
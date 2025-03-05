package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class PlanGenerationProducer(
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(PlanGenerationProducer::class.java)

    fun sendPlanGenerationRequest(planId: String, userEmail: String) {
        logger.info("Enviando requisição de geração para plano: $planId, usuário: $userEmail")
        val message = PlanGenerationMessage(planId, userEmail)

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PLAN_GENERATION_EXCHANGE,
            RabbitMQConfig.PLAN_GENERATION_ROUTING_KEY,
            message
        )

        logger.info("Requisição de geração enviada com sucesso: $planId")
    }
}

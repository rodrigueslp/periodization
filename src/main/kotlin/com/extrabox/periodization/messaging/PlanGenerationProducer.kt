package com.extrabox.periodization.messaging

import com.extrabox.periodization.config.RabbitMQConfig
import com.extrabox.periodization.enums.PlanType
import com.extrabox.periodization.model.messaging.PlanGenerationMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class PlanGenerationProducer(
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(PlanGenerationProducer::class.java)

    fun sendPlanGenerationRequest(planId: String, userEmail: String, planType: PlanType) {
        logger.info("Enviando requisição de geração para plano: $planId, usuário: $userEmail, tipo: $planType")
        val message = PlanGenerationMessage(planId, userEmail, planType)

        val (routingKey, queue) = when (planType) {
            PlanType.CROSSFIT -> "plan.crossfit.generate" to "plan-generation-crossfit-queue"
            PlanType.STRENGTH -> "plan.strength.generate" to "plan-generation-strength-queue"
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PLAN_GENERATION_EXCHANGE,
            routingKey,
            message
        )

        logger.info("Requisição de geração enviada com sucesso: $planId")
    }

}

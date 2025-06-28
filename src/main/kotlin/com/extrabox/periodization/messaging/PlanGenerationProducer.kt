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
            PlanType.CROSSFIT -> RabbitMQConfig.PLAN_GENERATION_CROSSFIT_ROUTING_KEY to RabbitMQConfig.PLAN_GENERATION_CROSSFIT_QUEUE
            PlanType.STRENGTH -> RabbitMQConfig.PLAN_GENERATION_STRENGTH_ROUTING_KEY to RabbitMQConfig.PLAN_GENERATION_STRENGTH_QUEUE
            PlanType.RUNNING -> RabbitMQConfig.PLAN_GENERATION_RUNNING_ROUTING_KEY to RabbitMQConfig.PLAN_GENERATION_RUNNING_QUEUE
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PLAN_GENERATION_EXCHANGE,
            routingKey,
            message
        )

        logger.info("Requisição de geração enviada com sucesso: $planId para fila: $queue")
    }
}
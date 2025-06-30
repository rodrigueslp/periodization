package com.extrabox.periodization.config

import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    companion object {
        const val PLAN_GENERATION_EXCHANGE = "plan-generation-exchange"
        const val PLAN_GENERATION_CROSSFIT_QUEUE = "plan-generation-crossfit-queue"
        const val PLAN_GENERATION_STRENGTH_QUEUE = "plan-generation-strength-queue"
        const val PLAN_GENERATION_RUNNING_QUEUE = "plan-generation-running-queue"

        const val PLAN_GENERATION_CROSSFIT_ROUTING_KEY = "plan.crossfit.generate"
        const val PLAN_GENERATION_STRENGTH_ROUTING_KEY = "plan.strength.generate"
        const val PLAN_GENERATION_RUNNING_ROUTING_KEY = "plan.running.generate"
    }

    @Bean
    fun crossfitQueue() = Queue(PLAN_GENERATION_CROSSFIT_QUEUE)

    @Bean
    fun strengthQueue() = Queue(PLAN_GENERATION_STRENGTH_QUEUE)

    @Bean
    fun runningQueue() = Queue(PLAN_GENERATION_RUNNING_QUEUE)

    @Bean
    fun planExchange() = TopicExchange(PLAN_GENERATION_EXCHANGE)

    @Bean
    fun bindCrossfit() = BindingBuilder.bind(crossfitQueue())
        .to(planExchange())
        .with(PLAN_GENERATION_CROSSFIT_ROUTING_KEY)

    @Bean
    fun bindStrength() = BindingBuilder.bind(strengthQueue())
        .to(planExchange())
        .with(PLAN_GENERATION_STRENGTH_ROUTING_KEY)

    @Bean
    fun bindRunning() = BindingBuilder.bind(runningQueue())
        .to(planExchange())
        .with(PLAN_GENERATION_RUNNING_ROUTING_KEY)

    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter()
        return rabbitTemplate
    }
}

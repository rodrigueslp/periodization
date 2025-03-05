package com.extrabox.periodization.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class RabbitMQConfig {
    companion object {
        const val PLAN_GENERATION_QUEUE = "plan-generation-queue"
        const val PLAN_GENERATION_EXCHANGE = "plan-generation-exchange"
        const val PLAN_GENERATION_ROUTING_KEY = "plan.generate"
    }

    @Bean
    fun planGenerationQueue(): Queue {
        return Queue(PLAN_GENERATION_QUEUE, true)
    }

    @Bean
    fun planGenerationExchange(): TopicExchange {
        return TopicExchange(PLAN_GENERATION_EXCHANGE)
    }

    @Bean
    fun planGenerationBinding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(queue)
            .to(exchange)
            .with(PLAN_GENERATION_ROUTING_KEY)
    }

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

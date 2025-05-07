package com.extrabox.periodization.config

import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class AnthropicConfig {

    @Value("\${anthropic.api.key}")
    lateinit var apiKey: String

    @Value("\${anthropic.api.url}")
    lateinit var apiUrl: String

    @Value("\${anthropic.api.model}")
    lateinit var model: String

    @Bean
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()
    }
}

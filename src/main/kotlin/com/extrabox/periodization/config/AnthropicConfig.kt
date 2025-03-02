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
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

package com.extrabox.periodization.model

data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<Content>,
    val model: String,
    val stop_reason: String,
    val usage: Usage
)

data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)

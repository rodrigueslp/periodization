package com.extrabox.periodization.model

data class AnthropicRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 12000,
    val temperature: Double = 0.7,
    val system: String? = null
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String = "text",
    val text: String
)
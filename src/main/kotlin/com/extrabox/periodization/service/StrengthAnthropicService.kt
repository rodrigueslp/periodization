package com.extrabox.periodization.service

import com.extrabox.periodization.config.AnthropicConfig
import com.extrabox.periodization.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class StrengthAnthropicService(
    private val okHttpClient: OkHttpClient,
    private val anthropicConfig: AnthropicConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(StrengthAnthropicService::class.java)

    fun generateStrengthTrainingPlan(athleteData: StrengthAthleteData, planDuration: Int): String {
        val prompt = buildPrompt(athleteData, planDuration)
        val request = buildApiRequest(prompt)

        try {
            val response = callAnthropicApi(request)
            return parseResponseContent(response)
        } catch (e: Exception) {
            logger.error("Error calling Anthropic API for strength training plan", e)
            throw RuntimeException("Falha ao gerar o plano de musculação: ${e.message}")
        }
    }

    private fun buildPrompt(athleteData: StrengthAthleteData, planDuration: Int): String {
        return """
            Você é um especialista em musculação e programação de treinamento de força. Crie um plano de treino detalhado para um atleta com as seguintes características:
            
            Nome: ${athleteData.nome}
            Idade: ${athleteData.idade} anos
            Peso: ${athleteData.peso} kg
            Altura: ${athleteData.altura} cm
            Nível de experiência: ${athleteData.experiencia}
            Objetivo principal: ${athleteData.objetivo}
            Foco do treino: ${athleteData.trainingFocus}
            Disponibilidade semanal: ${athleteData.sessionsPerWeek} dias
            Duração de cada sessão: ${athleteData.sessionDuration} minutos
            
            ${if (!athleteData.objetivoDetalhado.isNullOrBlank()) "Objetivo detalhado: ${athleteData.objetivoDetalhado}" else ""}
            ${if (!athleteData.lesoes.isNullOrBlank()) "Lesões ou limitações: ${athleteData.lesoes}" else ""}
            ${if (!athleteData.historico.isNullOrBlank()) "Histórico de treino: ${athleteData.historico}" else ""}
            ${if (!athleteData.equipmentAvailable.isNullOrBlank()) "Equipamentos disponíveis: ${athleteData.equipmentAvailable}" else ""}
            ${if (!athleteData.periodoTreino.isNullOrBlank()) "O atleta irá treinar sempre no período: ${athleteData.periodoTreino}" else ""}
            
            Crie uma periodização para ${planDuration} semanas, dividida em fases específicas para atingir o objetivo do atleta.
            Não use 1RM como parametro, pois diferente de um atleta de crossfit, nem todos os praticantes de musculação sabem o seu RM.
            
            Para cada semana inclua:
            1. Objetivos da semana
            2. Detalhes dos treinos para cada dia
            3. Divisão dos grupamentos musculares
            4. Exercícios específicos com séries, repetições e cargas recomendadas
            5. Tempo de descanso entre séries
            6. Técnicas avançadas de treinamento (quando apropriado)
            7. Recomendações de recuperação
            
            Formate a resposta como uma programação detalhada, semana por semana, com todos os detalhes necessários para o atleta seguir.
        """.trimIndent()
    }

    private fun buildApiRequest(prompt: String): AnthropicRequest {
        val message = Message(
            role = "user",
            content = listOf(Content(text = prompt))
        )

        return AnthropicRequest(
            model = anthropicConfig.model,
            messages = listOf(message),
            system = "Você é um especialista em programação de treinamento para musculação e força. Forneça planos detalhados e personalizados baseados nas informações dos atletas."
        )
    }

    private fun callAnthropicApi(request: AnthropicRequest): AnthropicResponse {
        val json = objectMapper.writeValueAsString(request)
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(anthropicConfig.apiUrl)
            .addHeader("x-api-key", anthropicConfig.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody)
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code} - ${response.body?.string()}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            return objectMapper.readValue(responseBody, AnthropicResponse::class.java)
        }
    }

    private fun parseResponseContent(response: AnthropicResponse): String {
        return response.content
            .filter { it.type == "text" }
            .joinToString("\n") { it.text }
    }
}

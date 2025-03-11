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
class AnthropicService(
    private val okHttpClient: OkHttpClient,
    private val anthropicConfig: AnthropicConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AnthropicService::class.java)

    fun generateTrainingPlan(athleteData: AthleteData, planDuration: Int): String {
        val prompt = buildPrompt(athleteData, planDuration)
        val request = buildApiRequest(prompt)

        try {
            val response = callAnthropicApi(request)
            return parseResponseContent(response)
        } catch (e: Exception) {
            logger.error("Error calling Anthropic API", e)
            throw RuntimeException("Falha ao gerar o plano de treinamento: ${e.message}")
        }
    }

    private fun buildPrompt(athleteData: AthleteData, planDuration: Int): String {
        return """
            Você é um especialista em CrossFit e programação de treinamento. Crie uma planilha de periodização detalhada para um atleta com as seguintes características:
            
            Nome: ${athleteData.nome}
            Idade: ${athleteData.idade} anos
            Peso: ${athleteData.peso} kg
            Altura: ${athleteData.altura} cm
            Nível de experiência: ${athleteData.experiencia}
            Objetivo principal: ${athleteData.objetivo}
            Disponibilidade semanal: ${athleteData.disponibilidade} dias
            
            ${if (!athleteData.objetivoDetalhado.isNullOrBlank()) "Objetivo detalhado: ${athleteData.objetivoDetalhado}" else ""}
            ${if (!athleteData.lesoes.isNullOrBlank()) "Lesões ou limitações: ${athleteData.lesoes}" else ""}
            ${if (!athleteData.historico.isNullOrBlank()) "Histórico de treino: ${athleteData.historico}" else ""}
            
            ${if (!athleteData.treinoPrincipal) "Este será meu treino principal do atleta" else "Este será meu treino secundário do atleta... um extra para o treino do box"}
            
            ${if (!athleteData.periodoTreino.isNullOrBlank()) "O atleta irá treinar sempre no período: ${athleteData.periodoTreino}" else ""}
            
            ${buildBenchmarksInfo(athleteData)}
            
            Crie uma periodização para ${planDuration} semanas, dividida em fases específicas para atingir o objetivo do atleta.
            
            Para cada semana inclua:
            1. Objetivos da semana
            2. Detalhes dos treinos para cada dia
            3. Intensidade de cada treino (baixa, média, alta)
            4. Exercícios específicos com séries, repetições e cargas recomendadas (em % do 1RM)
            5. WODs (Workout of the Day) específicos
            6. Recomendações de recuperação
            
            Formate a resposta como uma programação detalhada, semana por semana, com todos os detalhes necessários para o atleta seguir.
        """.trimIndent()
    }

    private fun buildBenchmarksInfo(athleteData: AthleteData): String {
        val benchmarks = athleteData.benchmarks ?: return "Sem dados de benchmarks disponíveis."

        val benchmarksList = mutableListOf<String>()

        benchmarks.backSquat?.let { benchmarksList.add("Back Squat 1RM: $it kg") }
        benchmarks.deadlift?.let { benchmarksList.add("Deadlift 1RM: $it kg") }
        benchmarks.clean?.let { benchmarksList.add("Clean 1RM: $it kg") }
        benchmarks.snatch?.let { benchmarksList.add("Snatch 1RM: $it kg") }
        benchmarks.fran?.let { benchmarksList.add("Fran: $it") }
        benchmarks.grace?.let { benchmarksList.add("Grace: $it") }

        return if (benchmarksList.isNotEmpty()) {
            "Benchmarks:\n" + benchmarksList.joinToString("\n")
        } else {
            "Sem dados de benchmarks disponíveis."
        }
    }

    private fun buildApiRequest(prompt: String): AnthropicRequest {
        val message = Message(
            role = "user",
            content = listOf(Content(text = prompt))
        )

        return AnthropicRequest(
            model = anthropicConfig.model,
            messages = listOf(message),
            system = "Você é um especialista em programação de treinamento para CrossFit. Forneça periodizações detalhadas e personalizadas baseadas nas informações dos atletas."
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

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
class RunningAnthropicService(
    private val okHttpClient: OkHttpClient,
    private val anthropicConfig: AnthropicConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RunningAnthropicService::class.java)

    fun generateRunningTrainingPlan(athleteData: RunningAthleteData, planDuration: Int): String {
        val prompt = buildPrompt(athleteData, planDuration)
        val request = buildApiRequest(prompt)

        try {
            val response = callAnthropicApi(request)
            return parseResponseContent(response)
        } catch (e: Exception) {
            logger.error("Error calling Anthropic API for running training plan", e)
            throw RuntimeException("Falha ao gerar o plano de corrida: ${e.message}")
        }
    }

    private fun buildPrompt(athleteData: RunningAthleteData, planDuration: Int): String {
        val performanceInfo = buildPerformanceInfo(athleteData)
        val targetInfo = buildTargetInfo(athleteData)
        val preferencesInfo = buildPreferencesInfo(athleteData)

        return """
            Você é um especialista em corrida e treinamento de endurance. Crie um plano de treino detalhado e progressivo para um corredor com as seguintes características:
            
            **INFORMAÇÕES PESSOAIS:**
            Nome: ${athleteData.nome}
            Idade: ${athleteData.idade} anos
            Peso: ${athleteData.peso} kg
            Altura: ${athleteData.altura} cm
            Nível de experiência: ${athleteData.experiencia}
            
            **OBJETIVO E META:**
            Objetivo principal: ${athleteData.objetivo}
            $targetInfo
            
            **DISPONIBILIDADE E VOLUME ATUAL:**
            Dias disponíveis por semana: ${athleteData.diasDisponiveis}
            Volume semanal atual: ${athleteData.volumeSemanalAtual} km
            
            **PERFORMANCE ATUAL:**
            $performanceInfo
            
            **PREFERÊNCIAS E LIMITAÇÕES:**
            $preferencesInfo
            ${if (!athleteData.historicoLesoes.isNullOrBlank()) "Histórico de lesões: ${athleteData.historicoLesoes}" else ""}
            ${if (!athleteData.experienciaAnterior.isNullOrBlank()) "Experiência anterior: ${athleteData.experienciaAnterior}" else ""}
            
            **INSTRUÇÕES PARA O PLANO:**
            Crie uma periodização para ${planDuration} semanas, seguindo os princípios de treinamento de corrida:
            
            1. **Estrutura de cada semana deve incluir:**
               - Objetivo específico da semana
               - Volume total em quilômetros
               - Distribuição dos treinos por dia
               - Tipos de treino (base aeróbica, intervalados, tempo run, longão, recuperação)
               - Intensidades específicas (ritmos/paces recomendados)
               - Aquecimento e volta à calma para cada sessão
               - Dias de descanso ou recuperação ativa
            
            2. **Tipos de treino para incluir:**
               - Corridas base (aeróbica leve)
               - Corridas em ritmo moderado
               - Intervalados (400m, 800m, 1000m, etc)
               - Tempo runs (corridas em ritmo de prova)
               - Corridas longas progressivas
               - Fartlek
               - Treinos de subida
               - Corridas de recuperação
            
            3. **Periodização deve seguir:**
               - Fase de base aeróbica (primeiras semanas)
               - Fase de desenvolvimento (meio do programa)
               - Fase de intensificação (semanas finais)
               - Semana de regeneração a cada 3-4 semanas
               ${if (!athleteData.dataProva.isNullOrBlank()) "- Taper adequado antes da prova" else ""}
            
            4. **Para cada treino específico, inclua:**
               - Duração/distância exata
               - Pace/ritmo alvo (baseado na performance atual)
               - Estrutura (aquecimento + parte principal + volta à calma)
               - Instruções de percepção de esforço
               - Dicas técnicas quando necessário
            
            5. **Considerações especiais:**
               - Progressão gradual de volume (regra dos 10%)
               - Prevenção de lesões
               - Hidratação e nutrição específica para treinos longos
               - Equipamentos recomendados
               - Sinais de alerta para overtraining
            
            Formate a resposta como uma periodização detalhada, semana por semana, com todos os detalhes necessários para o corredor seguir o plano com segurança e efetividade.
            
            Seja específico com paces/ritmos, sempre baseado na performance atual do atleta e progressivo em direção ao objetivo.
        """.trimIndent()
    }

    private fun buildPerformanceInfo(athleteData: RunningAthleteData): String {
        val performances = mutableListOf<String>()

        athleteData.paceAtual5k?.let { performances.add("Pace atual 5k: $it/km") }
        athleteData.paceAtual10k?.let { performances.add("Pace atual 10k: $it/km") }
        athleteData.melhorTempo5k?.let { performances.add("Melhor tempo 5k: $it") }
        athleteData.melhorTempo10k?.let { performances.add("Melhor tempo 10k: $it") }
        athleteData.melhorTempo21k?.let { performances.add("Melhor tempo 21k: $it") }
        athleteData.melhorTempo42k?.let { performances.add("Melhor tempo 42k: $it") }

        return if (performances.isNotEmpty()) {
            performances.joinToString("\n")
        } else {
            "Sem dados de performance anteriores disponíveis."
        }
    }

    private fun buildTargetInfo(athleteData: RunningAthleteData): String {
        val targets = mutableListOf<String>()

        athleteData.tempoObjetivo?.let { targets.add("Tempo objetivo: $it") }
        athleteData.dataProva?.let { targets.add("Data da prova: $it") }

        return if (targets.isNotEmpty()) {
            targets.joinToString("\n")
        } else {
            ""
        }
    }

    private fun buildPreferencesInfo(athleteData: RunningAthleteData): String {
        val preferences = mutableListOf<String>()

        athleteData.preferenciaTreino?.let { preferences.add("Preferência de horário: $it") }
        athleteData.localTreino?.let { preferences.add("Local de treino preferido: $it") }
        athleteData.equipamentosDisponiveis?.let { preferences.add("Equipamentos disponíveis: $it") }

        return if (preferences.isNotEmpty()) {
            preferences.joinToString("\n")
        } else {
            ""
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
            system = "Você é um especialista em treinamento de corrida e endurance. Forneça planos detalhados, científicos e personalizados baseados na fisiologia do exercício e metodologias comprovadas de treinamento de corrida."
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

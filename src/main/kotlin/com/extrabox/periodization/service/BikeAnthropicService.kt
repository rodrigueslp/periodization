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
class BikeAnthropicService(
    private val okHttpClient: OkHttpClient,
    private val anthropicConfig: AnthropicConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(BikeAnthropicService::class.java)

    fun generateBikeTrainingPlan(athleteData: BikeAthleteData, planDuration: Int): String {
        val prompt = buildPrompt(athleteData, planDuration)
        val request = buildApiRequest(prompt)

        try {
            val response = callAnthropicApi(request)
            return parseResponseContent(response)
        } catch (e: Exception) {
            logger.error("Error calling Anthropic API for bike training plan", e)
            throw RuntimeException("Falha ao gerar o plano de bike: ${e.message}")
        }
    }

    private fun buildPrompt(athleteData: BikeAthleteData, planDuration: Int): String {
        val performanceInfo = buildPerformanceInfo(athleteData)
        val targetInfo = buildTargetInfo(athleteData)
        val preferencesInfo = buildPreferencesInfo(athleteData)

        return """
            Você é um especialista em ciclismo e treinamento de endurance. Crie um plano de treino detalhado e progressivo para um ciclista com as seguintes características:
            
            **INFORMAÇÕES PESSOAIS:**
            Nome: ${athleteData.nome}
            Idade: ${athleteData.idade} anos
            Peso: ${athleteData.peso} kg
            Altura: ${athleteData.altura} cm
            Nível de experiência: ${athleteData.experiencia}
            
            **OBJETIVO E META:**
            Objetivo principal: ${athleteData.objetivo}
            $targetInfo
            
            **CAPACIDADE ATUAL:**
            Dias disponíveis por semana: ${athleteData.diasDisponiveis}
            Volume semanal atual: ${athleteData.volumeSemanalAtual} horas
            $performanceInfo
            
            **PREFERÊNCIAS E RECURSOS:**
            $preferencesInfo
            
            **INFORMAÇÕES ADICIONAIS:**
            ${if (!athleteData.historicoLesoes.isNullOrBlank()) "Histórico de lesões: ${athleteData.historicoLesoes}" else ""}
            ${if (!athleteData.experienciaAnterior.isNullOrBlank()) "Experiência anterior: ${athleteData.experienciaAnterior}" else ""}
            
            **INSTRUÇÕES PARA O PLANO:**
            
            1. Crie um plano progressivo de $planDuration semanas
            2. Respeite o nível de experiência e o volume atual do atleta
            3. Include diferentes tipos de treino: base aeróbica, intervalados, força, recuperação
            4. Considere zonas de treinamento baseadas em FTP se disponível
            5. Adapte os treinos conforme o tipo de bike e objetivos
            6. Include orientações sobre nutrição e hidratação
            7. Considere períodos de recuperação adequados
            8. Forneça alternativas para dias de mau tempo se necessário
            
            **FORMATO DA RESPOSTA:**
            
            Estruture o plano da seguinte forma:
            
            # PLANO DE TREINO DE BIKE - $planDuration SEMANAS
            
            ## RESUMO EXECUTIVO
            - Objetivo: [objetivo principal]
            - Duração: $planDuration semanas
            - Volume semanal: [progressão do volume]
            - Intensidade: [distribuição das zonas]
            
            ## PERIODIZAÇÃO GERAL
            [Explique a divisão do período em fases - base, build, especialização, etc.]
            
            ## ZONAS DE TREINAMENTO
            [Defina as zonas baseadas em FTP, frequência cardíaca ou percepção de esforço]
            
            ## PLANO SEMANAL DETALHADO
            
            ### SEMANA 1-${minOf(4, planDuration)}
            **Foco: [Fase inicial - base aeróbica]**
            
            **Segunda-feira:** [Descrição do treino]
            **Terça-feira:** [Descrição do treino]
            **Quarta-feira:** [Descrição do treino]
            **Quinta-feira:** [Descrição do treino]
            **Sexta-feira:** [Descrição do treino]
            **Sábado:** [Descrição do treino]
            **Domingo:** [Descrição do treino]
            
            [Continue para todas as semanas, agrupando por fases similares]
            
            ## ORIENTAÇÕES IMPORTANTES
            
            ### Nutrição e Hidratação
            [Orientações específicas para ciclismo]
            
            ### Equipamentos
            [Recomendações baseadas nos equipamentos disponíveis]
            
            ### Monitoramento
            [Como acompanhar o progresso - FTP, tempos, sensações]
            
            ### Prevenção de Lesões
            [Exercícios complementares, alongamentos, fortalecimento]
            
            ### Adaptações para Clima
            [Alternativas para treino indoor/outdoor]
            
            ## TESTES E AVALIAÇÕES
            [Quando fazer testes de FTP, como acompanhar o progresso]
            
            ## DICAS FINAIS
            [Recomendações gerais, motivação, ajustes do plano]
            
            Certifique-se de que o plano seja:
            - Progressivo e realista
            - Adaptado ao nível do atleta
            - Claro e fácil de seguir
            - Específico para os objetivos
            - Flexível para ajustes
        """.trimIndent()
    }

    private fun buildPerformanceInfo(athleteData: BikeAthleteData): String {
        val performanceParts = mutableListOf<String>()
        
        athleteData.ftpAtual?.let { 
            performanceParts.add("FTP atual: $it watts")
        }
        
        athleteData.potenciaMediaAtual?.let { 
            performanceParts.add("Potência média atual: $it watts")
        }
        
        athleteData.melhorTempo40km?.let { 
            performanceParts.add("Melhor tempo 40km: $it")
        }
        
        athleteData.melhorTempo100km?.let { 
            performanceParts.add("Melhor tempo 100km: $it")
        }
        
        athleteData.melhorTempo160km?.let { 
            performanceParts.add("Melhor tempo 160km: $it")
        }
        
        return if (performanceParts.isNotEmpty()) {
            "Performance atual:\n" + performanceParts.joinToString("\n") { "- $it" }
        } else {
            "Performance atual: Não informada"
        }
    }

    private fun buildTargetInfo(athleteData: BikeAthleteData): String {
        val targetParts = mutableListOf<String>()
        
        athleteData.tempoObjetivo?.let { 
            targetParts.add("Tempo objetivo: $it")
        }
        
        athleteData.dataProva?.let { 
            targetParts.add("Data da prova: $it")
        }
        
        return if (targetParts.isNotEmpty()) {
            targetParts.joinToString("\n") { "- $it" }
        } else {
            ""
        }
    }

    private fun buildPreferencesInfo(athleteData: BikeAthleteData): String {
        val preferenceParts = mutableListOf<String>()
        
        athleteData.tipoBike?.let { 
            preferenceParts.add("Tipo de bike: $it")
        }
        
        athleteData.preferenciaTreino?.let { 
            preferenceParts.add("Preferência de treino: $it")
        }
        
        athleteData.equipamentosDisponiveis?.let { 
            preferenceParts.add("Equipamentos disponíveis: $it")
        }
        
        athleteData.zonaTreinoPreferida?.let { 
            preferenceParts.add("Zona de treino preferida: $it")
        }
        
        return if (preferenceParts.isNotEmpty()) {
            preferenceParts.joinToString("\n") { "- $it" }
        } else {
            "Preferências não especificadas"
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
            system = "Você é um especialista em ciclismo e treinamento de endurance. Forneça planos detalhados, científicos e personalizados baseados na fisiologia do exercício e metodologias comprovadas de treinamento de ciclismo."
        )
    }

    private fun callAnthropicApi(request: AnthropicRequest): AnthropicResponse {
        val requestBody = objectMapper.writeValueAsString(request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(anthropicConfig.apiUrl)
            .header("x-api-key", anthropicConfig.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        logger.info("Calling Anthropic API for bike training plan generation")

        okHttpClient.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                logger.error("Anthropic API call failed with status ${response.code}: $errorBody")
                throw IOException("Falha na chamada da API Anthropic: ${response.code} - $errorBody")
            }

            val responseBody = response.body?.string()
                ?: throw IOException("Resposta vazia da API Anthropic")

            logger.info("Successfully received response from Anthropic API")
            return objectMapper.readValue(responseBody, AnthropicResponse::class.java)
        }
    }

    private fun parseResponseContent(response: AnthropicResponse): String {
        return response.content
            .filter { it.type == "text" }
            .joinToString("\n") { it.text }
    }
}

package com.extrabox.periodization.service

import com.extrabox.periodization.model.RunningAthleteData
import com.itextpdf.html2pdf.HtmlConverter
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class RunningPdfGenerationService {
    private val logger = LoggerFactory.getLogger(RunningPdfGenerationService::class.java)

    @Value("\${app.logo.url:http://localhost:3000/static/media/planilize-logo.854402b5f477121cfbd7.png}")
    private lateinit var logoUrl: String

    fun generatePdf(athleteData: RunningAthleteData, planContent: String): ByteArray {
        try {
            val html = buildHtmlFromMarkdown(athleteData, planContent)
            val outputStream = ByteArrayOutputStream()
            HtmlConverter.convertToPdf(ByteArrayInputStream(html.toByteArray()), outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Erro ao gerar arquivo PDF para corrida", e)
            throw RuntimeException("Falha ao gerar o plano de corrida em PDF", e)
        }
    }

    private fun enhanceMarkdown(markdown: String): String {
        val weekPattern = Regex("^(?i)(semana|week)\\s+(\\d+)(.*)$", RegexOption.MULTILINE)
        return markdown.replace(weekPattern) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val number = matchResult.groupValues[2]
            val rest = matchResult.groupValues[3]
            "## <span class=\"week-header\">$prefix $number$rest</span>"
        }
    }

    private fun buildHtmlFromMarkdown(athleteData: RunningAthleteData, markdownContent: String): String {
        val extensions = listOf(TablesExtension.create(), HeadingAnchorExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(enhanceMarkdown(markdownContent))
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        val contentHtml = renderer.render(document)

        val headerHtml = if (logoUrl.isNotBlank()) {
            """
            <div class="header">
                <div class="logo-container">
                    <img src="$logoUrl" alt="Logo" class="logo" onerror="this.style.display='none'">
                </div>
                <div class="title-container">
                    <h1 class="title">PLANO DE TREINAMENTO DE CORRIDA</h1>
                </div>
            </div>
            """
        } else {
            """
            <div class="header">
                <h1 class="title">PLANO DE TREINAMENTO DE CORRIDA</h1>
            </div>
            """
        }

        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Plano de Treinamento de Corrida</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 20px;
                }
                
                .header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 30px;
                    border-bottom: 2px solid #dc2626;
                    padding-bottom: 15px;
                }
                .logo-container {
                    width: 30%;
                }
                .logo {
                    max-width: 150px;
                    max-height: 70px;
                }
                .title-container {
                    width: 70%;
                    text-align: right;
                }
                .title {
                    font-size: 24px;
                    color: #991b1b;
                    margin: 0;
                }
                
                h1 {
                    color: #dc2626;
                    font-size: 24px;
                    margin-top: 20px;
                    border-bottom: 1px solid #e5e7eb;
                    padding-bottom: 10px;
                }
                h2 {
                    color: #b91c1c;
                    font-size: 20px;
                    margin-top: 16px;
                }
                h3 {
                    color: #991b1b;
                    font-size: 18px;
                    margin-top: 14px;
                }
                p {
                    margin: 12px 0;
                }
                ul, ol {
                    margin-top: 10px;
                    margin-bottom: 10px;
                    padding-left: 30px;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 15px 0;
                }
                th, td {
                    border: 1px solid #d1d5db;
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: #fef2f2;
                }
                .athlete-info {
                    background-color: #fef2f2;
                    padding: 15px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    border: 1px solid #fecaca;
                }
                .athlete-info h2 {
                    color: #dc2626;
                    margin-top: 0;
                }
                .athlete-info p {
                    margin: 5px 0;
                }
                .section-title {
                    background-color: #fee2e2;
                    padding: 8px 15px;
                    border-radius: 5px;
                    margin-top: 25px;
                    margin-bottom: 15px;
                }
                .performance-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    grid-gap: 10px;
                }
                .week-header {
                    background-color: #fecaca;
                    padding: 8px;
                    border-left: 4px solid #dc2626;
                    margin-top: 20px;
                }
                code {
                    background-color: #f1f5f9;
                    padding: 2px 4px;
                    border-radius: 3px;
                }
                strong {
                    color: #991b1b;
                }
                .footer {
                    margin-top: 30px;
                    text-align: center;
                    font-size: 12px;
                    color: #666;
                    border-top: 1px solid #e5e7eb;
                    padding-top: 10px;
                }
                .performance-section {
                    background-color: #fffbeb;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    border: 1px solid #fed7aa;
                }
                .goals-section {
                    background-color: #f0fdf4;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    border: 1px solid #bbf7d0;
                }
            </style>
        </head>
        <body>
            $headerHtml
            
            <div class="athlete-info">
                <h2>Informações do Corredor</h2>
                <p><strong>Nome:</strong> ${athleteData.nome}</p>
                <p><strong>Idade:</strong> ${athleteData.idade} anos</p>
                <p><strong>Peso:</strong> ${athleteData.peso} kg</p>
                <p><strong>Altura:</strong> ${athleteData.altura} cm</p>
                <p><strong>Nível de Experiência:</strong> ${athleteData.experiencia}</p>
                <p><strong>Objetivo Principal:</strong> ${athleteData.objetivo}</p>
                <p><strong>Dias Disponíveis:</strong> ${athleteData.diasDisponiveis} por semana</p>
                <p><strong>Volume Atual:</strong> ${athleteData.volumeSemanalAtual} km/semana</p>
                
                ${renderPerformanceSection(athleteData)}
                ${renderGoalsSection(athleteData)}
                ${renderPreferencesSection(athleteData)}
            </div>
            
            <h2 class="section-title">PLANO DE TREINAMENTO</h2>
            
            <div class="plan-content">
                $contentHtml
            </div>
            
            <div class="footer">
                <p>Gerado em $currentDate</p>
            </div>
        </body>
        </html>
        """
    }

    private fun renderPerformanceSection(athleteData: RunningAthleteData): String {
        val performances = mutableListOf<String>()

        athleteData.paceAtual5k?.let { performances.add("<p><strong>Pace atual 5k:</strong> $it/km</p>") }
        athleteData.paceAtual10k?.let { performances.add("<p><strong>Pace atual 10k:</strong> $it/km</p>") }
        athleteData.melhorTempo5k?.let { performances.add("<p><strong>Melhor tempo 5k:</strong> $it</p>") }
        athleteData.melhorTempo10k?.let { performances.add("<p><strong>Melhor tempo 10k:</strong> $it</p>") }
        athleteData.melhorTempo21k?.let { performances.add("<p><strong>Melhor tempo 21k:</strong> $it</p>") }
        athleteData.melhorTempo42k?.let { performances.add("<p><strong>Melhor tempo 42k:</strong> $it</p>") }

        return if (performances.isNotEmpty()) {
            """
            <div class="performance-section">
                <h3>Performance Atual</h3>
                <div class="performance-grid">
                    ${performances.joinToString("\n")}
                </div>
            </div>
            """
        } else {
            ""
        }
    }

    private fun renderGoalsSection(athleteData: RunningAthleteData): String {
        val goals = mutableListOf<String>()

        athleteData.tempoObjetivo?.let { goals.add("<p><strong>Tempo Objetivo:</strong> $it</p>") }
        athleteData.dataProva?.let { goals.add("<p><strong>Data da Prova:</strong> $it</p>") }

        return if (goals.isNotEmpty()) {
            """
            <div class="goals-section">
                <h3>Objetivos e Metas</h3>
                ${goals.joinToString("\n")}
            </div>
            """
        } else {
            ""
        }
    }

    private fun renderPreferencesSection(athleteData: RunningAthleteData): String {
        val preferences = mutableListOf<String>()

        athleteData.preferenciaTreino?.let { preferences.add("<p><strong>Horário Preferido:</strong> $it</p>") }
        athleteData.localTreino?.let { preferences.add("<p><strong>Local de Treino:</strong> $it</p>") }
        athleteData.equipamentosDisponiveis?.let { preferences.add("<p><strong>Equipamentos:</strong> $it</p>") }
        athleteData.historicoLesoes?.let { preferences.add("<p><strong>Histórico de Lesões:</strong> $it</p>") }
        athleteData.experienciaAnterior?.let { preferences.add("<p><strong>Experiência Anterior:</strong> $it</p>") }

        return if (preferences.isNotEmpty()) {
            """
            <div style="margin-top: 15px;">
                <h3>Preferências e Observações</h3>
                ${preferences.joinToString("\n")}
            </div>
            """
        } else {
            ""
        }
    }
}

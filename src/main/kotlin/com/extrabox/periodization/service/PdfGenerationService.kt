package com.extrabox.periodization.service

import com.extrabox.periodization.model.AthleteData
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
class PdfGenerationService {
    private val logger = LoggerFactory.getLogger(PdfGenerationService::class.java)

    // URL da logo - você pode configurar isso no application.properties
    @Value("\${app.logo.url:http://localhost:3000/static/media/planilize-logo.854402b5f477121cfbd7.png}")
    private lateinit var logoUrl: String

    fun generatePdf(athleteData: AthleteData, planContent: String): ByteArray {
        try {
            // Criar o HTML combinando informações do atleta e o conteúdo do plano com formatação Markdown
            val html = buildHtmlFromMarkdown(athleteData, planContent)

            // Converter o HTML para PDF usando iText
            val outputStream = ByteArrayOutputStream()
            HtmlConverter.convertToPdf(ByteArrayInputStream(html.toByteArray()), outputStream)

            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Erro ao gerar arquivo PDF", e)
            throw RuntimeException("Falha ao gerar o plano de treinamento em PDF", e)
        }
    }

    private fun enhanceMarkdown(markdown: String): String {
        // Regex para detectar cabeçalhos de semanas (Semana X: ou Week X:)
        val weekPattern = Regex("^(?i)(semana|week)\\s+(\\d+)(.*)$", RegexOption.MULTILINE)

        // Substituir por cabeçalhos formatados
        return markdown.replace(weekPattern) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val number = matchResult.groupValues[2]
            val rest = matchResult.groupValues[3]

            // Formatar como um cabeçalho H2 com estilo especial
            "## <span class=\"week-header\">$prefix $number$rest</span>"
        }
    }

    private fun buildHtmlFromMarkdown(athleteData: AthleteData, markdownContent: String): String {
        // Configurar o Parser CommonMark com extensões
        val extensions = listOf(
            TablesExtension.create(),
            HeadingAnchorExtension.create()
        )
        val parser = Parser.builder()
            .extensions(extensions)
            .build()

        // Converter markdown para HTML
        val document = parser.parse(enhanceMarkdown(markdownContent))
        val renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()
        val contentHtml = renderer.render(document)

        // Verificar se há URL da logo
        val headerHtml = if (logoUrl.isNotBlank()) {
            """
            <div class="header">
                <div class="logo-container">
                    <img src="$logoUrl" alt="Logo" class="logo" onerror="this.style.display='none'">
                </div>
                <div class="title-container">
                    <h1 class="title">PLANO DE PERIODIZAÇÃO DE CROSSFIT</h1>
                </div>
            </div>
            """
        } else {
            """
            <div class="header">
                <h1 class="title">PLANO DE PERIODIZAÇÃO DE CROSSFIT</h1>
            </div>
            """
        }

        // Data atual formatada
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        // Construir o HTML completo com CSS para estilizar
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Plano de Periodização de CrossFit</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 20px;
                }
                
                /* Estilos do cabeçalho com logo */
                .header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 30px;
                    border-bottom: 2px solid #2563eb;
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
                    color: #1e3a8a;
                    margin: 0;
                }
                
                h1 {
                    color: #2563eb;
                    font-size: 24px;
                    margin-top: 20px;
                    border-bottom: 1px solid #e5e7eb;
                    padding-bottom: 10px;
                }
                h2 {
                    color: #1e40af;
                    font-size: 20px;
                    margin-top: 16px;
                }
                h3 {
                    color: #1e3a8a;
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
                    background-color: #f3f4f6;
                }
                .athlete-info {
                    background-color: #f9fafb;
                    padding: 15px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    border: 1px solid #e5e7eb;
                }
                .athlete-info h2 {
                    color: #2563eb;
                    margin-top: 0;
                }
                .athlete-info p {
                    margin: 5px 0;
                }
                .section-title {
                    background-color: #e0e7ff;
                    padding: 8px 15px;
                    border-radius: 5px;
                    margin-top: 25px;
                    margin-bottom: 15px;
                }
                .benchmarks {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    grid-gap: 10px;
                }
                .week-header {
                    background-color: #dbeafe;
                    padding: 8px;
                    border-left: 4px solid #2563eb;
                    margin-top: 20px;
                }
                code {
                    background-color: #f1f5f9;
                    padding: 2px 4px;
                    border-radius: 3px;
                }
                strong {
                    color: #1e3a8a;
                }
                .footer {
                    margin-top: 30px;
                    text-align: center;
                    font-size: 12px;
                    color: #666;
                    border-top: 1px solid #e5e7eb;
                    padding-top: 10px;
                }
            </style>
        </head>
        <body>
            $headerHtml
            
            <div class="athlete-info">
                <h2>Informações do Atleta</h2>
                <p><strong>Nome:</strong> ${athleteData.nome}</p>
                <p><strong>Idade:</strong> ${athleteData.idade} anos</p>
                <p><strong>Peso:</strong> ${athleteData.peso} kg</p>
                <p><strong>Altura:</strong> ${athleteData.altura} cm</p>
                <p><strong>Nível de Experiência:</strong> ${athleteData.experiencia}</p>
                <p><strong>Objetivo Principal:</strong> ${athleteData.objetivo}</p>
                <p><strong>Disponibilidade:</strong> ${athleteData.disponibilidade} dias por semana</p>
                
                ${if (!athleteData.lesoes.isNullOrBlank()) "<p><strong>Lesões:</strong> ${athleteData.lesoes}</p>" else ""}
                ${if (!athleteData.historico.isNullOrBlank()) "<p><strong>Histórico de Treino:</strong> ${athleteData.historico}</p>" else ""}
                
                ${renderBenchmarks(athleteData)}
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

    private fun renderBenchmarks(athleteData: AthleteData): String {
        val benchmarks = athleteData.benchmarks ?: return ""

        val benchmarkRows = mutableListOf<String>()

        benchmarks.backSquat?.let { benchmarkRows.add("<p><strong>Back Squat 1RM:</strong> $it kg</p>") }
        benchmarks.deadlift?.let { benchmarkRows.add("<p><strong>Deadlift 1RM:</strong> $it kg</p>") }
        benchmarks.clean?.let { benchmarkRows.add("<p><strong>Clean 1RM:</strong> $it kg</p>") }
        benchmarks.snatch?.let { benchmarkRows.add("<p><strong>Snatch 1RM:</strong> $it kg</p>") }
        benchmarks.fran?.let { benchmarkRows.add("<p><strong>Fran:</strong> $it</p>") }
        benchmarks.grace?.let { benchmarkRows.add("<p><strong>Grace:</strong> $it</p>") }

        return if (benchmarkRows.isNotEmpty()) {
            """
            <div class="benchmarks-section">
                <h3>Benchmarks</h3>
                <div class="benchmarks">
                    ${benchmarkRows.joinToString("\n")}
                </div>
            </div>
            """
        } else {
            ""
        }
    }
}
